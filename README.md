# Capital Gains — tax calculation on stock trades

A **Java + Spring Boot study project**. It calculates the tax owed on the profit
(or loss) of stock buy and sell trades, following the rules of the "Capital
Gains" challenge.

Three ways to use it:

| Way | For what |
|-----|----------|
| **Broker UI** (`/`) | log in, buy and sell tickers, and see average price, profit, exemption and tax on every sale |
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
mvn test                 # 46 tests
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

### Docker

```bash
docker compose up --build        # UI + API on port 8080, capped at 256 MB / 1 CPU; data kept in a volume
docker build -t capital-gains .
docker run -i --rm capital-gains --spring.profiles.active=cli < examples/input.txt
```

## Endpoints

| Method | Route | Description |
|--------|-------|-------------|
| `POST` | `/api/taxes/orders` | submit one simulation · **202** + `Location: /api/taxes/orders/{id}` |
| `POST` | `/api/taxes/orders/batch` | submit a basket of simulations · **202** |
| `GET`  | `/api/taxes/orders/{id}` | poll one order (`PENDING` → `PROCESSING` → `COMPLETED`/`FAILED`) |
| `GET`  | `/api/simulations?page=&size=` | paginated history (summary) |
| `GET`  | `/api/simulations/{id}` | detail: each trade and the tax it generated |

### Broker (session login)

| Method | Route | Description |
|--------|-------|-------------|
| `POST` | `/api/auth/register` | create a **USER** account (password: 6+ characters, no other rule) and log in · **201** |
| `POST` | `/api/auth/login` / `/api/auth/logout` | start / end the session (`JSESSIONID` cookie) |
| `GET`  | `/api/auth/me` | the logged-in user, role and session info (login time, idle timeout) · **401** without a session |
| `POST` | `/api/auth/password` | change the password; needs the current one (**400** if wrong) |
| `GET`  | `/api/broker/tickers` | listed tickers open for buying, with a reference price |
| `GET`  | `/api/broker/rules` | the fee schedule and tax rules new orders are placed under |
| `GET`  | `/api/broker/account` | totals, positions per ticker, trade log with the full tax breakdown |
| `POST` | `/api/broker/quote` | preview a trade (result, exemption, tax) without executing it |
| `POST` | `/api/broker/trades` | execute a trade · **201**, **422** when selling more than held or the ticker is not tradable |

### Admin (role ADMIN · **403** otherwise)

| Method | Route | Description |
|--------|-------|-------------|
| `GET` / `POST` | `/api/admin/users` | list users / create one with a role · **409** if the username is taken |
| `PUT`  | `/api/admin/users/{id}/role` | change a user's role · **409** for your own |
| `GET` / `POST` | `/api/admin/tickers` | whole catalog / list a new ticker · **409** if it exists |
| `PUT`  | `/api/admin/tickers/{symbol}` | edit name and reference price; `active: false` delists (holders can still sell) |
| `GET` / `PUT` | `/api/admin/rules` | fee type (`FIXED` $ per order or `PERCENT` of the order value), fee, tax rate %, exemption limit |

**Roles.** A `USER` trades and manages only their own account (theme, password).
An `ADMIN` also manages users and roles, the ticker catalog, and the broker-wide
fees and tax rules. The role is read from the database on every call, so a change
takes effect immediately. An admin cannot change their own role, which keeps at
least one admin around. The **first admin** is created on startup from
`broker.admin.username` / `broker.admin.password` (env `BROKER_ADMIN_USERNAME` /
`BROKER_ADMIN_PASSWORD`); with no password set, a random one is generated and
printed once in the log.

Each ticker is its own position (average price and loss carried forward).
A **brokerage fee** (set by an admin: fixed per order or a % of the order value)
is charged on every buy and sell: it adds to a buy's cost, so it raises the
average price, and it comes out of a sale's proceeds, so it lowers the taxable
profit. The exemption looks at the gross sale value. Each trade stores the fee
and the tax rules it was placed under, so an admin changing them never rewrites
history. The challenge API and the CLI always use the challenge's rules.
Only the raw trades are stored; results and taxes are replayed through the same
`TaxCalculator`, so the broker and the challenge API can never disagree.

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
- **In-memory** H2 by default (`mvn spring-boot:run`, tests): gone when the application stops.
  `docker compose` switches it to a file on a volume, so broker users and trades persist.
- `totalTax` and `tradeCount` are denormalized on `Simulation` (immutable once
  created), so the listing does not load the collection of items.
- The async queue is the `simulation_order` **table itself** (outbox pattern),
  not an external broker — no infra to run. `OrderProcessor` polls it;
  `OrderExecution` is a separate bean so its `@Transactional` boundary actually
  applies. Details in [`docs/async-orders.md`](docs/async-orders.md).
