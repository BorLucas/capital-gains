# Ganho de Capital — cálculo de imposto sobre operações de ações

Projeto de **estudo de Java + Spring Boot**. Calcula o imposto devido sobre o
lucro (ou prejuízo) de operações de compra e venda de ações, seguindo as regras
do desafio "Ganho de Capital".

Três formas de uso:

| Forma | Para quê |
|-------|----------|
| **API REST** | uso principal; calcula e guarda o histórico |
| **Swagger UI** (`/swagger-ui.html`) | testar a API pelo navegador |
| **CLI** (`--spring.profiles.active=cli`) | formato original do desafio: lê JSON do stdin |

## Stack

- Java 21 · Spring Boot 3.3.5 · Maven
- `spring-boot-starter-web`, `-validation`, `-data-jpa`
- H2 (banco em memória) · springdoc-openapi (Swagger UI)
- Testes: JUnit 5 + AssertJ + MockMvc + **ArchUnit** (regras de arquitetura)

## Como rodar

JDK 21 e Maven já estão instalados e no PATH do usuário. Numa sessão antiga:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;C:\Users\drume\tools\apache-maven-3.9.9\bin;$env:Path"
```

```bash
mvn test                 # 43 testes
mvn spring-boot:run      # sobe a API na porta 8080
mvn -DskipTests package  # gera o jar
```

### Swagger UI / Console H2

- <http://localhost:8080/swagger-ui.html> · spec em `/v3/api-docs`
- <http://localhost:8080/h2-console> — JDBC URL `jdbc:h2:mem:ganhodecapital`, user `sa`, sem senha

### Modo CLI

```powershell
cmd /c "java -jar target\ganho-de-capital-0.0.1-SNAPSHOT.jar --spring.profiles.active=cli < exemplos\entrada.txt"
```

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/impostos/simulacao` | calcula uma simulação · **201** + `Location: /api/simulacoes/{id}` |
| `POST` | `/api/impostos/lote` | várias simulações independentes, tudo numa transação |
| `GET`  | `/api/simulacoes?page=&size=` | histórico paginado (resumo) |
| `GET`  | `/api/simulacoes/{id}` | detalhe: cada operação e o imposto que gerou |

`POST /api/impostos/simulacao`

```json
[
  {"operation": "buy",  "unit-cost": 10.00, "quantity": 10000},
  {"operation": "sell", "unit-cost": 20.00, "quantity": 5000},
  {"operation": "sell", "unit-cost": 5.00,  "quantity": 5000}
]
```
→ `201 Created`, `Location: /api/simulacoes/1`
```json
[{"tax": 0.00}, {"tax": 10000.00}, {"tax": 0.00}]
```

Erros seguem RFC 7807 (`application/problem+json`): `400` entrada inválida,
`422` venda maior que a carteira, `404` simulação inexistente.

Exemplos prontos em [`requests.http`](requests.http).

## Regras implementadas

1. **20%** sobre o lucro das vendas.
2. **Compra** não paga imposto; atualiza o **preço médio ponderado**
   (arredondado a 2 casas). Carteira zerada → a próxima compra reinicia o preço médio.
3. **Prejuízo é sempre acumulado** (mesmo em venda isenta) e abatido de lucros futuros.
4. **Isenção** quando `unit-cost × quantity ≤ R$ 20.000`: sem imposto, e um lucro
   isento **não** consome o prejuízo acumulado.
5. Vender mais do que há em carteira → erro (`422`).

Os 9 casos oficiais estão em
[`CalculadoraDeImpostoTest`](src/test/java/com/estudos/ganhodecapital/domain/CalculadoraDeImpostoTest.java).

## Arquitetura

```
domain/                núcleo Java puro (sem Spring/JPA) — testado pelo ArchUnit
  Dinheiro, Operacao, Imposto, ResultadoOperacao, TipoOperacao
  carteira/            agregado Carteira + máquina de estados (EstadoCarteira, EventoOperacao)
  erro/                hierarquia GanhoDeCapitalException
formato/               DTOs do formato do desafio, compartilhados por web e cli
application/            casos de uso (CalcularImpostoService, HistoricoService)
historico/             persistência JPA + read models (projeção de resumo, detalhe)
web/                   controllers REST + tradução de erros (RFC 7807)
cli/                   adapter de entrada por stdin (perfil "cli")
config/                Clock, OpenAPI
```

A `Carteira` é uma pequena **máquina de estados** — ver
[`docs/maquina-de-estados.md`](docs/maquina-de-estados.md). As regras de camada
(domínio não depende de framework, web e cli não se conhecem, etc.) são
verificadas em
[`ArquiteturaTest`](src/test/java/com/estudos/ganhodecapital/arquitetura/ArquiteturaTest.java).

## Decisões de modelagem

- **Lucro isento não abate prejuízo acumulado** — única interpretação que fecha
  os casos 6 e 9 juntos.
- `Dinheiro` centraliza a regra de arredondamento (2 casas, `HALF_UP`).
- Histórico em H2 **em memória**: some quando a aplicação para.
- `impostoTotal` e `quantidadeOperacoes` são desnormalizados na `Simulacao`
  (imutável após criada), para a listagem não carregar a coleção de itens.
