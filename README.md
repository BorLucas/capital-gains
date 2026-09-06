# Capital Gains — tax calculation on stock trades

A **Java + Spring Boot study project**. It calculates the tax owed on the profit
(or loss) of stock buy and sell trades, following the rules of the "Capital
Gains" challenge.

Three ways to use it:

| Way | For what |
|-----|----------|
| **REST API** | main use; calculates and stores the history |
| **Swagger UI** (`/swagger-ui.html`) | try the API from the browser |
| **CLI** (`--spring.profiles.active=cli`) | the challenge's original format: reads JSON from stdin |

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

## Endpoints

| Method | Route | Description |
|--------|-------|-------------|
| `POST` | `/api/taxes/simulation` | calculates one simulation · **201** + `Location: /api/simulations/{id}` |
| `POST` | `/api/taxes/batch` | several independent simulations, all in one transaction |
| `GET`  | `/api/simulations?page=&size=` | paginated history (summary) |
| `GET`  | `/api/simulations/{id}` | detail: each trade and the tax it generated |

`POST /api/taxes/simulation`

```json
[
  {"operation": "buy",  "unit-cost": 10.00, "quantity": 10000},
  {"operation": "sell", "unit-cost": 20.00, "quantity": 5000},
  {"operation": "sell", "unit-cost": 5.00,  "quantity": 5000}
]
```
→ `201 Created`, `Location: /api/simulations/1`
```json
[{"tax": 0.00}, {"tax": 10000.00}, {"tax": 0.00}]
```

Errors follow RFC 7807 (`application/problem+json`): `400` invalid input,
`422` sell larger than the portfolio, `404` missing simulation.

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
[`TaxCalculatorTest`](src/test/java/com/example/capitalgains/domain/TaxCalculatorTest.java).

## Architecture

```
domain/                pure Java core (no Spring/JPA) — checked by ArchUnit
  Money, Trade, Tax, TradeResult, TradeType
  portfolio/            Portfolio aggregate + state machine (PortfolioState, TradeEvent)
  error/               CapitalGainsException hierarchy
format/                DTOs of the challenge format, shared by web and cli
application/            use cases (CalculateTaxService, HistoryService)
history/               JPA persistence + read models (summary and detail projections)
web/                   REST controllers + error translation (RFC 7807)
cli/                   stdin inbound adapter (profile "cli")
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
