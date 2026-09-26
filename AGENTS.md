# AGENTS.md

Orientation for AI agents working in this repo. Humans: see [`README.md`](README.md).

## What this is

A **Java 21 + Spring Boot 3.3.5** study project that calculates capital-gains
tax on stock trades (the "Capital Gains" challenge). It is deliberately
over-engineered for its size — the point is to practice clean architecture, a
domain state machine, and asynchronous processing, not to ship the smallest
solution.

Build tool: Maven. Persistence: in-memory H2 (wiped on restart). API docs:
springdoc / Swagger UI.

## Commands

```bash
mvn test                 # full suite (unit + @SpringBootTest); must stay green
mvn spring-boot:run      # API on :8080  (Swagger: /swagger-ui.html)
mvn -DskipTests package  # build target/capital-gains-0.0.1-SNAPSHOT.jar
java -jar target/capital-gains-0.0.1-SNAPSHOT.jar --spring.profiles.active=cli < examples/input.txt
```

There is no build wrapper (`mvnw`); a local Maven + JDK 21 is assumed.

## Hard conventions

1. **English only.** Every identifier, comment, Javadoc, error message, commit
   message, PR description, doc, test name — English. The only pre-existing
   non-English tokens are the challenge's JSON contract (`operation`,
   `unit-cost`, `quantity`, `tax`), which must not change. (The local folder is
   still named `ganho-de-capital`; the GitHub repo is `capital-gains`.)
2. **The domain package is pure Java.** `com.example.capitalgains.domain..` must
   not import Spring, JPA, Jackson, Hibernate, or any outer package. This is
   enforced by `ArchitectureTest` (ArchUnit) — run it before assuming a domain
   change is fine.
3. **Fail-fast in the domain.** Value objects and aggregates validate their
   invariants in the constructor / compact constructor. An invalid `Trade`,
   `Money`, etc. must never come to exist.
4. **Business errors are typed.** Everything extends
   `domain.error.CapitalGainsException`; never signal a business condition with a
   generic `IllegalArgumentException`. The web layer maps each type to a status
   in `web.ApiExceptionHandler` (RFC 7807 `application/problem+json`).
5. **Tests are part of the change.** New behavior needs a test; changed behavior
   updates one. The 9 official challenge cases in `TaxCalculatorTest` are the
   golden reference for the tax math — do not weaken them.
6. **Every change goes through a PR** with a descriptive English body. Commits
   end with the `Co-Authored-By` trailer already used in history.

## Architecture

Layers, innermost first. Dependencies point inward only (ArchUnit-checked).

```
domain/            pure Java core — the tax rules
  Money            money value object: 2dp, HALF_UP, centralizes rounding
  Trade            one buy/sell (type, unitCost: Money, quantity)
  Tax, TradeResult, TradeType
  TaxCalculator    walks a List<Trade>, returns List<TradeResult>
  portfolio/       Portfolio aggregate = state machine (see below)
  error/           CapitalGainsException hierarchy
format/            TradeJson / TaxJson — the challenge's JSON DTOs, shared by web + cli
orders/            async work queue: SimulationOrder entity, OrderStatus, OrderView, repo
application/        use cases / orchestration (Spring @Service beans)
  SubmitOrderService, OrderProcessor, OrderExecution, OrderQueryService, HistoryService
history/           JPA: Simulation entity + read models (SimulationDetail, SimulationSummary)
web/               REST controllers + ApiExceptionHandler
cli/               CliRunner — stdin adapter, profile "cli", synchronous
config/            Clock bean, OpenAPI bean
```

- `domain` and `format` know nothing of Spring or persistence.
- `web` and `cli` are independent inbound adapters — they must not reference each
  other.
- `history` and `orders` are infrastructure — the application depends on them,
  not vice versa.
- If you add a package, add or extend the matching ArchUnit rule.

## Domain: the tax rules

Encoded in `Portfolio` (a small explicit state machine) and validated by
`TaxCalculatorTest` + `PortfolioTest`.

- 20% tax on the profit of a sell.
- A **buy** pays no tax; it updates the weighted **average price** (2dp). When
  the position hits zero the state resets, so the next buy restarts the average.
- A **loss is always accumulated** and offsets future profits — even a loss on
  an exempt sell.
- **Exemption**: a sell whose `unitCost * quantity <= 20000` pays no tax. A
  *profit* on an exempt sell does **not** consume the accumulated loss. (This is
  the one non-obvious modeling call — it is the only reading that makes official
  cases 6 and 9 both pass. Do not "fix" it.)
- Selling more than the position throws `SellExceedsPortfolioException`.

`Portfolio` states: `EMPTY` <-> `HOLDING`. `apply(TradeEvent)` validates the
transition (guard) before running the action. Full write-up:
[`docs/state-machine.md`](docs/state-machine.md).

## The API is asynchronous (broker-style)

There is **no synchronous "calculate" endpoint**. A request is accepted and
queued; a background worker assesses it; the client polls.

```
POST /api/taxes/orders          -> 202, body = OrderView(status=PENDING), Location: /api/taxes/orders/{id}
POST /api/taxes/orders/batch    -> 202, one order per simulation (single tx: all or nothing)
GET  /api/taxes/orders/{id}     -> poll: PENDING -> PROCESSING -> COMPLETED | FAILED
GET  /api/simulations           -> paginated history of assessed simulations
GET  /api/simulations/{id}      -> the stored result a COMPLETED order links to (simulationUrl)
```

- Trades are validated at submission (`TradeJson.toDomain`) → malformed input is
  `400` and nothing is queued.
- `SellExceedsPortfolioException` depends on the *sequence*, so it is only found
  while assessing → the order is accepted (`202`) then settles `FAILED` with
  `failureReason`.

### How the queue works

The `simulation_order` table **is** the queue (outbox pattern) — no external
broker. `orders/`, `application/OrderProcessor`, `application/OrderExecution`.

- `OrderProcessor` — two `@Scheduled` methods: `drainQueue()` claims the oldest
  `PENDING` orders; `reapStuckOrders()` releases orders stuck in `PROCESSING`
  (worker died mid-assessment) back to `PENDING`, or `FAILED` after
  `orders.max-attempts`.
- `OrderExecution` — `claim` / `assess` / `fail`, **each its own transaction**,
  orchestrated by `OrderProcessor`. This is deliberate:
  - `claim` commits `PENDING -> PROCESSING` on its own, so `PROCESSING` is
    observable to a poller (a single transaction would hide it).
  - `assess` lets exceptions propagate (clean rollback, no half-written
    `Simulation`); `OrderProcessor` then calls `fail` in a **fresh** transaction.
    One poison order must never wedge the queue.
- Config: `orders.poll-interval-ms`, `orders.batch-size`,
  `orders.reaper-interval-ms`, `orders.processing-timeout`, `orders.max-attempts`.
- The worker is disabled under the `cli` profile.

Full write-up: [`docs/async-orders.md`](docs/async-orders.md).

## Gotchas

- **`@Transactional` self-invocation.** Spring's proxy is bypassed when a bean
  calls its own `@Transactional` method. That is why `OrderProcessor` (the
  `@Scheduled` loop) and `OrderExecution` (the transactional steps) are separate
  beans. Never collapse them. Same reason `HistoryService.record` is only ever
  called from another bean.
- **`Money` rounds on construction** (2dp, HALF_UP). Keep raw client precision
  out of it: `TradeJson` rejects a `unit-cost` with more than 2 decimal places
  rather than silently rounding it.
- **`SimulationOrder.version` is a boxed `Long` on purpose** — `null` tells
  Spring Data the entity is new, so `save()` does `persist()` not `merge()`
  (the id is application-assigned, so the id check alone can't tell).
- **Queue reads are id-only projections** (`findPendingIds` /
  `findStuckProcessingIds`). Don't turn them back into entity queries — the
  `trades` collection is EAGER and pagination + collection fetch degrades badly.
- **`Clock` is a bean.** Use it for "now" (`Instant.now(clock)`), never
  `Instant.now()`, so tests can reason about time.
- **The CLI stays synchronous.** It is a batch tool over stdin; the async model
  is for the HTTP API only.

## Where to add things

| You want to… | Put it in… |
|---|---|
| change a tax rule | `domain/portfolio/Portfolio` + `TaxCalculatorTest` / `PortfolioTest` |
| add a money operation | `domain/Money` |
| add / change an API route | `web/` + a `@SpringBootTest` MockMvc test |
| change the async lifecycle | `orders/SimulationOrder` (guards) + `application/OrderExecution` / `OrderProcessor` + `OrderProcessorTest` |
| add a persisted read model | `history/` (assessed simulations) or `orders/` (orders) |
| add config | `application.properties` + inject with `@Value` |

## Testing

- Unit: plain JUnit 5 + AssertJ for `domain` (no Spring).
- `@SpringBootTest` + MockMvc for the web flow; `OrderControllerTest` drives the
  real async path end to end with Awaitility (submit → the scheduler runs → poll
  to `COMPLETED`/`FAILED` → follow `simulationUrl`).
- `OrderProcessorTest` pushes the schedulers out to an hour and calls
  `claim` / `drainQueue` / `reapStuckOrders` by hand for deterministic coverage
  of the intermediate states and the reaper.
- `ArchitectureTest` enforces the layer rules — treat a failure there as a real
  regression, not a test to relax.
