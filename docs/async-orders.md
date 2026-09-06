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
            │  claims the oldest PENDING orders, hands each to OrderExecution          │
            └────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼  OrderExecution.execute(id)  @Transactional
        PENDING ──► PROCESSING ──► COMPLETED   (Simulation stored, order.simulationId set)
                               └─► FAILED      (SellExceedsPortfolioException → failureReason)
```

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

`OrderProcessor.drainQueue()` (the `@Scheduled` loop) calls
`OrderExecution.execute(id)`. If `execute` lived on `OrderProcessor`, that call
would be a plain in-object method call — Spring's proxy would not see it and
`@Transactional` would silently do nothing. Splitting the beans makes the
transaction boundary real: each order is assessed and its status flipped in
**one transaction**, so an order fills or is rejected atomically.

## Idempotency

`execute` re-reads the order and bails out unless it is still `PENDING`. If the
worker ran twice for the same id (restart mid-tick, a future second consumer),
the second run is a no-op. Order state only moves forward — `markProcessing` /
`markCompleted` / `markFailed` reject any out-of-order transition.

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
| `orders.batch-size` | `10` | orders claimed per tick |

The worker is disabled under the `cli` profile (`@Profile("!cli")`).

## Tests

[`OrderControllerTest`](../src/test/java/com/example/capitalgains/web/OrderControllerTest.java)
drives the real flow end to end: submit → the scheduler runs → Awaitility polls
the order until `COMPLETED` / `FAILED`, then follows `simulationUrl` to the
stored result.
