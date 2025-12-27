# tx-lab-mysql-hibernate

Laboratório didático para experimentar transações com MySQL usando Spring Boot 3 (Java 21), Hibernate/JPA, Flyway e Docker Compose.

## Conceitos

- **Transação física:** a unidade de commit/rollback controlada pelo banco de dados (um datasource, um transaction manager).
- **Transação lógica:** uma operação de negócio que pode envolver **mais de uma transação física** (ex.: dois bancos diferentes). Sem coordenação distribuída, uma falha parcial deixa dados inconsistentes.
- **Objetivo do lab:** mostrar a diferença entre as duas camadas e como o *outbox pattern* ajuda quando não queremos XA/JTA.

## Requisitos

- Java 21
- Maven
- Docker + Docker Compose

## Subindo a infraestrutura

```bash
docker compose up -d single-mysql       # cenário 1
docker compose up -d core-mysql audit-mysql  # cenários 2 e 3
```

Credenciais padrão: `root` / `rootpass`.

## Perfis disponíveis

- `single-mysql` (padrão): um MySQL na porta **3308** com dois schemas (`txlab_core`, `txlab_billing`).
- `dual-mysql`: dois MySQLs – core na porta **3310** e audit na porta **3311**. Sem XA/JTA.

Ative o perfil desejado ao rodar a aplicação:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=single-mysql
./mvnw spring-boot:run -Dspring-boot.run.profiles=dual-mysql
```

## Migrations

Flyway mantém esquemas separados:

- `src/main/resources/db/migration/core` → `txlab_core` (users, outbox_event)
- `db/migration/billing` → `txlab_billing` (invoices)
- `db/migration/audit` → `txlab_audit` (audit_log)
- `db/migration/core` → tabelas core + artefatos de propagação/locks

## Cenário 1 — Single MySQL (1 transação física)

- Endpoint: `POST /single/users-with-invoice`
- Fluxo: cria usuário (`txlab_core.users`) + invoice (`txlab_billing.invoices`) dentro da **mesma** transação anotada com `@Transactional`.
- Parâmetro `fail=true` lança `RuntimeException` entre os passos.

Exemplos:

```bash
curl -X POST "http://localhost:8080/single/users-with-invoice?fail=false" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ana"}'
# Esperado: commit em users e invoices

curl -X POST "http://localhost:8080/single/users-with-invoice?fail=true" \
  -H "Content-Type: application/json" \
  -d '{"name":"Bruno"}'
# Esperado: rollback em ambos os schemas
```

## Cenário 2 — Dual MySQL (sem XA)

- Endpoint: `POST /dual/users-with-audit`
- Fluxo: cria usuário no core, **commita**, depois tenta gravar audit no banco audit. `fail=true` simula falha antes do commit do audit.
- Resultado: falha no audit mantém o usuário criado (transação lógica não é atômica).

Exemplos:

```bash
curl -X POST "http://localhost:8080/dual/users-with-audit?fail=false" \
  -H "Content-Type: application/json" \
  -d '{"name":"Carla"}'
# Esperado: usuário + audit gravados (duas transações físicas)

curl -X POST "http://localhost:8080/dual/users-with-audit?fail=true" \
  -H "Content-Type: application/json" \
  -d '{"name":"Diego"}'
# Esperado: usuário criado, audit ausente (inconsistência controlada para estudo)
```

## Cenário 3 — Outbox Pattern (solução sem XA)

- Endpoint: `POST /outbox/users`
- Fluxo (perfil `dual-mysql`):
  1. Mesma transação **core** grava o usuário e um registro em `txlab_core.outbox_event` com `status=PENDING`.
  2. Worker agendado (`OutboxWorker`) roda a cada 5s: lê pendências, grava audit no segundo banco e marca evento como `PROCESSED`.
- Se o audit falhar, o evento permanece `PENDING` e será reprocessado sem perder o commit do core.

Exemplo:

```bash
curl -X POST "http://localhost:8080/outbox/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Eva"}'
# Esperado: usuário + outbox pendente; worker processa e cria audit posteriormente
```

## Logs úteis

- Início/commit/rollback das operações são logados nos serviços (`SingleTransactionService`, `DualTransactionService`, `OutboxWorker`).
- O worker informa quantos eventos pendentes foram encontrados e quais foram processados ou mantidos como pendentes.

## Estrutura de pastas (resumo)

- `docker-compose.yml` e `docker/init` — infraestrutura MySQL.
- `src/main/java/com/txlab/single` — cenário 1.
- `src/main/java/com/txlab/dual` — cenário 2.
- `src/main/java/com/txlab/outbox` — cenário 3 (outbox + worker).
- `src/main/java/com/txlab/config/DualDataSourceConfig.java` — datasources/EMFs para `dual-mysql`.

## Novos cenários avançados (perfil `single-mysql`)

### Propagation / "transação em cima da outra"

**Endpoints** (catálogo `txlab_core` e `txlab_billing` via `@Table(catalog=...)`):

- `POST /single/advanced/propagation/requires-new`
  - Body: `{"name":"Fulano","items":["A","B","C"],"failOuter":false,"failOnItem":"B"}`
  - Outer REQUIRED cria usuário; cada item roda com `@Transactional(REQUIRES_NEW)` para gravar `batch_item` + `local_audit`.
  - `failOnItem` faz rollback apenas do item falho. `failOuter=true` rollbacka o usuário, mas mantém commits dos blocos `REQUIRES_NEW`.
  - Logs mostram início/commit/rollback de cada bloco.

- `POST /single/advanced/propagation/nested-savepoint`
  - Body: `{"name":"Fulano","failInner":true}`
  - Outer REQUIRED cria usuário; nested `@Transactional(NESTED)` tenta criar invoice.
  - `failInner=true` rollbacka só o savepoint (invoice), usuário commita. `failInner=false` commita usuário e invoice.
  - Se o dialeto não suportar `NESTED`, o log indicará o erro e o outer continua.

**Consultas úteis**

```sql
SELECT * FROM txlab_core.batch_item ORDER BY id DESC;
SELECT * FROM txlab_core.local_audit ORDER BY id DESC;
SELECT * FROM txlab_core.users ORDER BY id DESC;
SELECT * FROM txlab_billing.invoices ORDER BY id DESC;
```

**Exemplos curl**

```bash
# Itens commitarão mesmo se outer falhar
curl -X POST http://localhost:8080/single/advanced/propagation/requires-new \
  -H "Content-Type: application/json" \
  -d '{"name":"Joao","items":["A","B","C"],"failOuter":true,"failOnItem":"B"}'

# Nested com savepoint
curl -X POST http://localhost:8080/single/advanced/propagation/nested-savepoint \
  -H "Content-Type: application/json" \
  -d '{"name":"Maria","failInner":false}'

curl -X POST http://localhost:8080/single/advanced/propagation/nested-savepoint \
  -H "Content-Type: application/json" \
  -d '{"name":"Pedro","failInner":true}'
```

### Locks / concorrência (SELECT FOR UPDATE)

Tabelas novas: `txlab_core.wallet` (seed inicial id=1 balance=100.00, id=2 para deadlock).

**Endpoints**

- `POST /single/advanced/locks/hold-for-update?sleepMs=15000`
  - Abre transação, faz `SELECT ... FOR UPDATE` no wallet 1, dorme, incrementa balance e commita. Útil para segurar o lock.
- `POST /single/advanced/locks/try-update`
  - Tenta atualizar o wallet 1; fica bloqueado até o lock ser liberado. Loga tempo de espera.
- `POST /single/advanced/locks/deadlock/start-a` e `/start-b`
  - Sequência cruzada em wallets 1 e 2 para provocar deadlock. Um dos lados receberá erro de deadlock; mensagem aparece nos logs e na resposta HTTP.

**Roteiro manual**

1. Janela A: `curl -X POST "http://localhost:8080/single/advanced/locks/hold-for-update?sleepMs=20000"`
2. Janela B durante o sleep: `curl -X POST "http://localhost:8080/single/advanced/locks/try-update"` (só termina após o lock ser liberado).

Deadlock:

```
curl -X POST http://localhost:8080/single/advanced/locks/deadlock/start-a
curl -X POST http://localhost:8080/single/advanced/locks/deadlock/start-b
```

### Isolamento / leitura consistente

**Endpoints**

- `GET /single/advanced/isolation/repeatable-read?sleepMs=15000`
- `GET /single/advanced/isolation/read-committed?sleepMs=15000`

Ambos fazem `COUNT(*)` de `txlab_core.users`, dormem e contam novamente. Compare `firstCount` vs `secondCount`.

**Roteiro em duas janelas**

1. Janela 1: chamar o endpoint (mantém transação aberta com `sleepMs`).
2. Janela 2: criar um usuário no meio (pode ser `/single/users-with-invoice?fail=false`).
3. Ver resultado:
   - REPEATABLE_READ tende a manter `firstCount==secondCount`.
   - READ_COMMITTED deve enxergar o novo registro (`secondCount` maior).

## Observações

- Nenhum XA/JTA é usado; cada datasource possui seu próprio `PlatformTransactionManager`.
- `spring.profiles.default=single-mysql` para facilitar o primeiro teste.
