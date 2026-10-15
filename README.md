# FilaFácil Logística

Backend da operação da FilaFácil: coletas, clientes, entregas e reembolsos, janelas de coleta, agendamento de docas e manifestos de carga.

Kotlin puro (sem framework): servidor HTTP do JDK, JDBC com HikariCP, Flyway e Jackson. Banco PostgreSQL.

Fluxos, regras de negócio e o que é validado pelo sistema ou pela operação estão em [`docs/regras-de-negocio.md`](docs/regras-de-negocio.md).

## Pré-requisitos

- JDK 21. Se não estiver instalado, o Gradle baixa automaticamente.
- Docker, para o Postgres local e para os testes.

## Rodando localmente

```bash
docker compose up -d     # Postgres em localhost:5432
./gradlew run            # API em http://localhost:8080
```

As migrations (`src/main/resources/db/migration`) rodam na inicialização e já carregam os dados importados das planilhas da operação.

Testes:

```bash
./gradlew test
```

O arquivo [`http/filafacil.http`](http/filafacil.http) tem exemplos de todas as chamadas (IntelliJ ou extensão REST Client do VS Code).

## Explorando localmente

### Collection Postman / Insomnia

Em [`http/postman`](http/postman) há uma collection (Postman v2.1) com todas as rotas, casos de erro e asserts de status, e um environment local (`baseUrl = http://localhost:8080`).

- **Postman:** *Import* → selecione `filafacil.postman_collection.json` e `filafacil-local.postman_environment.json` → escolha o environment *FilaFácil Local*.
- **Insomnia:** *Import* → `filafacil.postman_collection.json` (o Insomnia converte o formato do Postman). Crie a variável `baseUrl` no environment se ela não vier na importação.
- **Linha de comando:**
  ```bash
  npx newman run http/postman/filafacil.postman_collection.json \
    -e http/postman/filafacil-local.postman_environment.json
  ```

Rode as pastas em ordem. As requests que alteram dados criam as próprias entidades e guardam os IDs em variáveis da collection (`pickupId`, `refundId`, `dockId`...), então a collection pode ser executada várias vezes sem reset.

### Dados do seed

Não há seed separado: as migrations [`V4__seed_operational_data.sql`](src/main/resources/db/migration/V4__seed_operational_data.sql) (clientes, coletas, entregas, reembolsos, pagamentos) e [`V5__create_dock_appointments.sql`](src/main/resources/db/migration/V5__create_dock_appointments.sql) (docas) carregam os dados importados das planilhas na primeira subida da API. Pontos de partida úteis:

| Cenário | IDs |
|---|---|
| Clientes inativos (não aceitam coleta) | clientes 9 e 16 |
| Coletas `EXPIRED` (podem ser reagendadas) | 1, 13 e as que estavam `PENDING` (ver abaixo) |
| Coletas `CANCELED` | 8, 25, 30 |
| Coletas `SCHEDULED` / `IN_ROUTE` | 7, 17, 27, 28 / 10, 19, 20 |
| Coletas `COLLECTED` (transições bloqueadas) | 5, 9, 11, 12... |
| Entregas `DELAYED` | 4, 10, 20, 21, 33... |
| Entregas com status fora do padrão (`entregue`) | 44, 87, 119, 124 |
| Mesmo cliente/motorista com grafias diferentes | `Livraria Página Um` / `LIVRARIA PÁGINA UM`; `Joao Silva` / `JOÃO SILVA` |
| Reembolsos `APPROVED` / `REQUESTED` / `REJECTED` | 1–12 / 13–24 / 25–28 |
| Reembolso maior que o frete da entrega | 13 (entrega 126) |
| Reembolsos de entregas inexistentes | 29 (entrega 9999), 30 (entrega 1500) |
| Agendamentos de doca sobrepostos | 1 e 2 (doca 1, 05/10) |
| Janelas de coleta | `GET /pickup-windows?date=2026-10-06` |
| Manifestos | nenhum no seed; use `samples/manifests/*.json` |

Observações:

- As coletas `PENDING` do seed foram criadas antes de 03/10/2026. O job de expiração roda 1 minuto depois que a API sobe e as marca como `EXPIRED`. Para testar o fluxo de status, crie uma coleta nova (a collection já faz isso).
- Aprovar os reembolsos 13, 29 ou 30 retorna erro (422/404), mas deixa o reembolso gravado como `APPROVED`, porque a aprovação não roda em transação. Evite essas aprovações se quiser preservar o estado do seed.
- Para voltar ao estado inicial: `docker compose down -v && docker compose up -d` e `./gradlew run`.

## Configuração

Tudo fica em `src/main/resources/application.properties`:

| Chave | Uso |
|---|---|
| `server.port` | porta HTTP |
| `db.url`, `db.user`, `db.password`, `db.pool.size` | conexão com o Postgres |
| `prioritization.strategy` | priorização padrão das coletas: `package-count`, `customer-type` ou `manual` |
| `routing.enabled`, `routing.url` | envio das coletas em rota para o sistema de roteirização |
| `expiration.pending.max.hours`, `expiration.interval.minutes` | expiração automática de coletas pendentes |
| `uploads.dir` | pasta dos comprovantes e anexos |

## Endpoints

### Clientes
| Método | Rota |
|---|---|
| POST | `/customers` |
| GET | `/customers` |
| GET | `/customers/{id}` |
| PATCH | `/customers/{id}/deactivate` |

### Coletas
| Método | Rota | Observação |
|---|---|---|
| POST | `/pickups` | nasce `PENDING` |
| POST | `/pickups/batch` | importação em lote, com resultado por item |
| GET | `/pickups?status=&region=&customerType=&strategy=` | ordenado por prioridade |
| GET | `/pickups/{id}` | |
| PATCH | `/pickups/{id}/status` | `PENDING → SCHEDULED → IN_ROUTE → COLLECTED`; `CANCELED` a partir de `PENDING`/`SCHEDULED` |
| POST | `/pickups/{id}/reschedule` | cria uma nova coleta a partir de uma `CANCELED`/`EXPIRED` |
| DELETE | `/pickups/{id}` | |
| PUT / GET | `/pickups/{id}/proof` | comprovante de coleta (corpo binário) |
| GET | `/pickups/summary` | resumo operacional |

Coletas `PENDING` há mais de `expiration.pending.max.hours` horas viram `EXPIRED` automaticamente.

### Entregas e reembolsos
| Método | Rota |
|---|---|
| GET | `/deliveries?status=&driver=&customer=&from=&to=&hasRefund=&page=&size=` |
| GET | `/deliveries/{id}` |
| POST | `/deliveries/{id}/occurrences` |
| POST | `/refunds` |
| GET | `/refunds?status=` |
| GET | `/refunds/{id}` |
| POST | `/refunds/{id}/approve` |
| POST | `/refunds/{id}/reject` |
| GET | `/refunds/report` |

### Janelas de coleta
| Método | Rota |
|---|---|
| GET | `/pickup-windows?date=AAAA-MM-DD` |
| GET | `/pickup-windows/{id}` |

As janelas vêm de `src/main/resources/pickup-windows.csv`.

### Docas
| Método | Rota |
|---|---|
| POST | `/dock-appointments` |
| GET | `/dock-appointments` |
| PUT / GET | `/dock-appointments/{id}/attachment` (header `X-File-Name`) |

### Manifestos de carga
| Método | Rota |
|---|---|
| POST | `/manifests` |
| GET | `/manifests` |
| GET | `/manifests/{id}` |

Exemplos de manifestos exportados do WMS em [`samples/manifests`](samples/manifests).

## Processos da operação

- [Regras de negócio e fluxos](docs/regras-de-negocio.md)
- [Deploy em produção](docs/runbooks/deploy-manual.md)
- [Validação de manifesto de carga](docs/runbooks/validacao-de-manifesto.md)
- [Aprovação de reembolso](docs/runbooks/aprovacao-de-reembolso.md)
- [Atualização das janelas de coleta](docs/runbooks/atualizacao-de-janelas.md)
- [Agendamento de docas](docs/runbooks/agendamento-de-docas.md)
