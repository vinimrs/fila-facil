# FilaFácil Logística

Backend da operação da FilaFácil: coletas, clientes, entregas e reembolsos, janelas de coleta, agendamento de docas e manifestos de carga.

Kotlin puro (sem framework): servidor HTTP do JDK, JDBC com HikariCP, Flyway e Jackson. Banco PostgreSQL.

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

- [Deploy em produção](docs/runbooks/deploy-manual.md)
- [Validação de manifesto de carga](docs/runbooks/validacao-de-manifesto.md)
- [Aprovação de reembolso](docs/runbooks/aprovacao-de-reembolso.md)
- [Atualização das janelas de coleta](docs/runbooks/atualizacao-de-janelas.md)
- [Agendamento de docas](docs/runbooks/agendamento-de-docas.md)
