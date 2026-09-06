# Capital Gains — tax calculation on stock trades

A **Java + Spring Boot study project**. It calculates the tax owed on the profit
(or loss) of stock buy and sell trades, following the rules of the "Capital
Gains" challenge.

Three ways to use it:

| Way | For what |
|-----|----------|
| **REST API** | main use; submit an order, poll it until assessed, read the stored result |
| **Swagger UI** (`/swagger-ui.html`) | try the API from the browser |
| **CLI** (`--spring.profiles.active=cli`) | the challenge's original format: reads JSON from stdin, synchronous |

The API is **asynchronous**, modeled like placing broker orders: a request is
*accepted* (`202`) and queued, a background worker assesses it, and the client
polls the order until it is `COMPLETED` (with a link to the stored simulation)
or `FAILED` (with a reason). See [`docs/async-orders.md`](docs/async-orders.md).

## Stack

- Java 21 · Spring Boot 3.3.5 · Maven
- `spring-boot-starter-web`, `-validation`, `-data-jpa`
- H2 (in-memory database) · springdoc-openapi (Swagger UI)
- Tests: JUnit 5 + AssertJ + MockMvc + **ArchUnit** (architecture rules)

## How to run

```bash
mvn test                 # 42 tests
mvn spring-boot:run      # starts the API on port 8080
mvn -DskipTests package  # builds the jar
```

### Swagger UI / H2 console

- <http://localhost:8080/swagger-ui.html> · spec at `/v3/api-docs`
- <http://localhost:8080/h2-console> — JDBC URL `jdbc:h2:mem:capitalgains`, user `sa`, no password

### CLI mode

```bash
java -jar target/capital-gains-0.0.1-SNAPSHOT.jar --spring.profiles.active=cli < examples/input.txt
```

## Endpoints

| Method | Route | Description |
|--------|-------|-------------|
| `POST` | `/api/taxes/orders` | submit one simulation · **202** + `Location: /api/taxes/orders/{id}` |
| `POST` | `/api/taxes/orders/batch` | submit a basket of simulations · **202** |
| `GET`  | `/api/taxes/orders/{id}` | poll one order (`PENDING` → `PROCESSING` → `COMPLETED`/`FAILED`) |
| `GET`  | `/api/simulations?page=&size=` | paginated history (summary) |
| `GET`  | `/api/simulations/{id}` | detail: each trade and the tax it generated |

**1. Submit** `POST /api/taxes/orders`

```json
[
  {"operation": "buy",  "unit-cost": 10.00, "quantity": 10000},
  {"operation": "sell", "unit-cost": 20.00, "quantity": 5000},
  {"operation": "sell", "unit-cost": 5.00,  "quantity": 5000}
]
```
→ `202 Accepted`, `Location: /api/taxes/orders/1b4e...`
```json
{"id": "1b4e...", "status": "PENDING", "submittedAt": "..."}
```

**2. Poll** `GET /api/taxes/orders/1b4e...` until it settles

```json
{"id": "1b4e...", "status": "COMPLETED", "simulationId": 1,
 "simulationUrl": "/api/simulations/1", "finishedAt": "..."}
```

**3. Read the result** `GET /api/simulations/1`

```json
{"id": 1, "totalTax": 10000.00, "trades": [
  {"operation": "buy",  "unit-cost": 10.00, "quantity": 10000, "tax": 0.00},
  {"operation": "sell", "unit-cost": 20.00, "quantity": 5000,  "tax": 10000.00},
  {"operation": "sell", "unit-cost": 5.00,  "quantity": 5000,  "tax": 0.00}
]}
```

Trades are validated at submission (`400` on malformed input). A sell larger
than the position is only caught while assessing → the order ends `FAILED` with
`failureReason`. Errors follow RFC 7807 (`application/problem+json`).

Ready-made examples in [`requests.http`](requests.http).

## Implemented rules

1. **20%** on the profit of sells.
2. **Buys** pay no tax; they update the **weighted average price** (rounded to
   2 decimal places). An emptied portfolio → the next buy restarts the average price.
3. **A loss is always accumulated** (even on an exempt sell) and offset against
   future profits.
4. **Exemption** when `unit-cost × quantity ≤ 20,000`: no tax, and an exempt
   profit does **not** consume the accumulated loss.
5. Selling more than the portfolio holds → error (`422`).

The 9 official cases are in
[`TaxCalculatorTest`](src/test/java/com/example/capitalgains/domain/TaxCalculatorTest.java);
the async flow in
[`OrderControllerTest`](src/test/java/com/example/capitalgains/web/OrderControllerTest.java).

## Architecture

```
domain/                pure Java core (no Spring/JPA) — checked by ArchUnit
  Money, Trade, Tax, TradeResult, TradeType
  portfolio/            Portfolio aggregate + state machine (PortfolioState, TradeEvent)
  error/               CapitalGainsException hierarchy
format/                DTOs of the challenge format, shared by web and cli
orders/                SimulationOrder (the async work queue) + OrderStatus + read model
application/            use cases: SubmitOrderService, OrderProcessor (the worker),
                       OrderExecution, OrderQueryService, HistoryService
history/               JPA persistence + read models for the assessed simulations
web/                   REST controllers + error translation (RFC 7807)
cli/                   stdin inbound adapter (profile "cli", synchronous)
config/                Clock, OpenAPI
```

The `Portfolio` is a small **state machine** — see
[`docs/state-machine.md`](docs/state-machine.md). The layer rules (domain does
not depend on frameworks, web and cli do not know each other, etc.) are checked
in
[`ArchitectureTest`](src/test/java/com/example/capitalgains/architecture/ArchitectureTest.java).

## Modeling decisions

- **An exempt profit does not offset the accumulated loss** — the only reading
  that makes cases 6 and 9 pass together.
- `Money` centralizes the rounding rule (2 decimal places, `HALF_UP`) and
  rejects input with more precision at the edge.
- History in **in-memory** H2: gone when the application stops.
- `totalTax` and `tradeCount` are denormalized on `Simulation` (immutable once
  created), so the listing does not load the collection of items.
- The async queue is the `simulation_order` **table itself** (outbox pattern),
  not an external broker — no infra to run. `OrderProcessor` polls it;
  `OrderExecution` is a separate bean so its `@Transactional` boundary actually
  applies. Details in [`docs/async-orders.md`](docs/async-orders.md).
