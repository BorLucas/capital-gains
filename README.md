# Ganho de Capital — cálculo de imposto sobre operações de ações

Projeto de **estudo de Java + Spring Boot**. Calcula o imposto devido sobre o
lucro (ou prejuízo) de operações de compra e venda de ações, seguindo as regras
do desafio "Ganho de Capital".

Tem três formas de uso:

| Forma | Para quê |
|-------|----------|
| **API REST** | uso principal; calcula e guarda o histórico |
| **Swagger UI** (`/swagger-ui.html`) | testar a API pelo navegador, sem ferramenta externa |
| **CLI** (`--spring.profiles.active=cli`) | formato original do desafio: lê JSON do stdin |

## Stack

- Java 21 · Spring Boot 3.3.5 · Maven
- `spring-boot-starter-web`, `-validation`, `-data-jpa`
- H2 (banco em memória)
- springdoc-openapi (Swagger UI)
- Testes: JUnit 5 + AssertJ + MockMvc

## Como rodar

O JDK 21 (`C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot`) e o Maven
(`C:\Users\drume\tools\apache-maven-3.9.9`) já estão instalados e no PATH do
usuário. Num terminal novo já funciona; numa sessão antiga:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;C:\Users\drume\tools\apache-maven-3.9.9\bin;$env:Path"
```

```bash
mvn test                 # roda os 24 testes
mvn spring-boot:run      # sobe a API na porta 8080
mvn -DskipTests package  # gera o jar em target/
```

### Swagger UI

Com a API no ar: <http://localhost:8080/swagger-ui.html>
(spec crua em `/v3/api-docs`).

### Console do H2

<http://localhost:8080/h2-console> — JDBC URL `jdbc:h2:mem:ganhodecapital`,
usuário `sa`, sem senha. O banco zera a cada restart.

### Modo CLI

```powershell
# no PowerShell use o cmd para o redirecionamento "<"
cmd /c "java -jar target\ganho-de-capital-0.0.1-SNAPSHOT.jar --spring.profiles.active=cli < exemplos\entrada.txt"
```

```
[{"tax":0.00},{"tax":0.00},{"tax":0.00}]
[{"tax":0.00},{"tax":10000.00},{"tax":0.00}]
```

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/impostos/simulacao` | calcula uma lista de operações e salva no histórico |
| `POST` | `/api/impostos/lote` | várias simulações independentes de uma vez |
| `GET`  | `/api/simulacoes` | histórico (resumo), da mais recente para a mais antiga |
| `GET`  | `/api/simulacoes/{id}` | detalhe: cada operação e o imposto que gerou |

`POST /api/impostos/simulacao`

```json
[
  {"operation": "buy",  "unit-cost": 10.00, "quantity": 10000},
  {"operation": "sell", "unit-cost": 20.00, "quantity": 5000},
  {"operation": "sell", "unit-cost": 5.00,  "quantity": 5000}
]
```

```json
[{"tax": 0.00}, {"tax": 10000.00}, {"tax": 0.00}]
```

Exemplos prontos em [`requests.http`](requests.http).

## Regras implementadas

1. **Alíquota de 20%** sobre o lucro das operações de **venda**.
2. **Compra** não paga imposto, mas atualiza o **preço médio ponderado**:
   `novo = ((qtd_atual × médio_atual) + (qtd_comprada × preço_compra)) / (qtd_atual + qtd_comprada)`,
   arredondado para 2 casas decimais.
3. Quando a carteira zera, a próxima compra reinicia o preço médio.
4. **Prejuízo é sempre acumulado** (inclusive em vendas isentas) e abatido de
   lucros futuros **antes** de calcular o imposto.
5. **Isenção**: não há imposto quando o valor total da venda
   (`unit-cost × quantity`) é **≤ R$ 20.000,00**. Nessa situação o prejuízo
   continua acumulando, mas um lucro isento **não** consome o prejuízo acumulado.
6. Não é possível vender mais ações do que existem em carteira (retorna `400`).

Os 9 casos oficiais do desafio estão cobertos em
[`CalculadoraDeImpostoTest`](src/test/java/com/estudos/ganhodecapital/domain/CalculadoraDeImpostoTest.java).

## Estrutura

```
domain/      -> núcleo puro, sem Spring: Operacao, Imposto, CalculadoraDeImposto
service/     -> ImpostoService: calcula e manda para o histórico
historico/   -> Simulacao (@Entity), repositório JPA, HistoricoService, GET endpoints
web/         -> ImpostoController (POST), DTOs no formato do desafio, tratamento de erro
cli/         -> CliRunner: modo stdin (perfil "cli"), reaproveita o domain
config/      -> OpenApiConfig (metadados do Swagger)
```

O `domain` não conhece Spring, JSON nem banco — dá para testar e reaproveitar
isolado (é o que o `CliRunner` faz).

## Pontos em aberto / decisões de modelagem

- **Lucro isento não abate prejuízo acumulado.** O caso 9 do desafio só fecha
  com essa interpretação (a isenção dispensa o imposto *e* o abatimento). O
  prejuízo isento, por outro lado, continua acumulando (necessário para o caso 6).
- **Arredondamento**: `HALF_UP` para preço médio e para o imposto, ambos com 2 casas.
- **Valores monetários** usam `BigDecimal` em todo o fluxo.
- O histórico usa H2 **em memória**: some quando a aplicação para. Trocar para
  arquivo (`jdbc:h2:file:./data/...`) ou Postgres é só mudar o `application.properties`.
- O cálculo em si é *stateless*; cada requisição parte do zero.
