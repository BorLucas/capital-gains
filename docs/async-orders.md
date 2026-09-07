# Asynchronous order processing

Tax assessment is not done in the request thread. A request is **accepted and
queued**, a background worker assesses it, and the client polls the order —
the same shape as placing an order with a broker.

```
POST /api/taxes/orders
  │  trades validated at the edge (400 on malformed input)
  ▼
SimulationOrder(status = PENDING)  ──stored──►  simulation_order table  ( = the queue )
  │
  │  202 Accepted, Location: /api/taxes/orders/{id}
  ▼
client polls GET /api/taxes/orders/{id}

            ┌─────────────── OrderProcessor (@Scheduled, every 200 ms) ───────────────┐
            │  finds the oldest PENDING order ids, then per order:                    │
            │    execution.claim(id)   —— tx 1:  PENDING  -> PROCESSING  (committed)  │
            │    execution.assess(id)  —— tx 2:  run calc + store Simulation          │
            │                                    PROCESSING -> COMPLETED              │
            │    on exception: execution.fail(id, reason) —— tx 3: PROCESSING -> FAILED│
            └────────────────────────────────────────────────────────────────────────┘

        PENDING ──► PROCESSING ──► COMPLETED   (Simulation stored, order.simulationId set)
                       │      └──► FAILED      (business rule broken, or internal error)
                       └──────────► PENDING    (reaper: worker never finished — see below)
```

Each step is its own transaction, so `PROCESSING` (and `startedAt`) is actually
committed and visible to a polling client — not collapsed into the same instant
as the final state. `assess` lets exceptions propagate so its transaction rolls
back cleanly (no half-written `Simulation`); the caller then records the failure
in a **fresh** transaction, which is why one poison order can no longer wedge the
queue in a rollback loop.

## Why the queue is a database table

The `simulation_order` table *is* the queue — the **outbox pattern**. Rows in
`PENDING` are the backlog. No RabbitMQ / Kafka to stand up: the project still
runs with a single `mvn spring-boot:run` and an in-memory H2.

Trade-offs, made on purpose for a study project:

| | This project | A real broker |
|---|---|---|
| Transport | DB table + polling | message broker, push |
| Consumers | one thread, submission order | competing consumers, partitions |
| Latency | poll interval (200 ms) | ~immediate |
| Delivery | at-least-once, idempotent claim | at-least-once + acks |

## Why `OrderExecution` is a separate bean

`OrderProcessor` (the `@Scheduled` loop) calls `OrderExecution.claim/assess/fail`.
If those lived on `OrderProcessor`, the calls would be plain in-object method
calls — Spring's proxy would not see them and `@Transactional` (including the
separate boundary per step) would silently do nothing. Splitting the beans makes
the transaction boundaries real.

## Idempotency and the reaper

`claim` bails out unless the order is still `PENDING`, so a double-run for the
same id is a no-op. Order state only moves forward — `markProcessing` /
`markCompleted` / `markFailed` reject any out-of-order transition.

If the process dies *between* `claim` and `assess`, the order is left
`PROCESSING` forever. `OrderProcessor.reapStuckOrders()` (a second `@Scheduled`
method) finds orders that have been `PROCESSING` past `orders.processing-timeout`
and releases them back to `PENDING` — unless they have already burned through
`orders.max-attempts`, in which case they are `FAILED`. `attempts` is bumped on
every `claim`.

## Failure semantics

- **Malformed trades** (bad `operation`, negative quantity, > 2 decimal places):
  rejected at submission with `400`. Nothing is queued.
- **`SellExceedsPortfolio`**: depends on the *sequence*, so it is only found
  while assessing. The order is accepted (`202`), then settles as `FAILED` with
  `failureReason`, e.g. `"selling 200 shares, but only 100 are held"`.
- **Unexpected errors**: logged and the order is `FAILED` with `"internal error"`.

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `orders.poll-interval-ms` | `200` | delay between worker ticks |
| `orders.batch-size` | `10` | orders claimed per tick / reaped per sweep |
| `orders.reaper-interval-ms` | `5000` | delay between reaper sweeps |
| `orders.processing-timeout` | `PT1M` | how long an order may stay `PROCESSING` before the reaper acts |
| `orders.max-attempts` | `3` | after this many `claim`s, a stuck order is `FAILED` instead of retried |

The worker is disabled under the `cli` profile (`@Profile("!cli")`).

## Tests

[`OrderControllerTest`](../src/test/java/com/example/capitalgains/web/OrderControllerTest.java)
drives the real flow end to end over HTTP: submit → the scheduler runs →
Awaitility polls the order until `COMPLETED` / `FAILED`, then follows
`simulationUrl` to the stored result.

[`OrderProcessorTest`](../src/test/java/com/example/capitalgains/application/OrderProcessorTest.java)
pushes the schedulers out to an hour and drives `claim` / `drainQueue` /
`reapStuckOrders` by hand, to assert the intermediate `PROCESSING` state, that a
failing order does not block the next one, and that a stuck order is released.
