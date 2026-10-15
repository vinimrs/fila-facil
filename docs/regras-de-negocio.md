# Regras de negócio da FilaFácil Logística

Este documento descreve o domínio da operação: quais são as entidades, como cada fluxo funciona e quais regras o sistema aplica de fato. Também registra o que hoje depende de processo manual (planilha, WhatsApp, banco), para que ninguém conte com uma validação que o sistema não faz.

Convenções usadas abaixo:

- **Sistema** — regra aplicada pela API; violar devolve erro.
- **Manual** — regra que existe na operação, mas que a API não verifica. Quem garante é a pessoa responsável, seguindo o runbook.
- **Lacuna** — comportamento atual que diverge do esperado ou deixa dados inconsistentes.

## Visão geral

A FilaFácil é uma operação de logística de última milha. O ciclo principal é:

```
Cliente ──solicita──▶ Coleta ──▶ CD (manifesto de carga) ──▶ Entrega ──▶ (Ocorrência) ──▶ Reembolso
                         ▲                                                                 │
                 Janelas de coleta                                                 Pagamento ao cliente
                                         Docas de descarga (transportadoras parceiras)
```

| Área | Responsável | Entidades |
|---|---|---|
| Clientes | comercial / SAC | `customers` |
| Coletas | planejamento de coletas | `pickups`, janelas de coleta (CSV) |
| Expedição | analista de expedição do CD | `manifests` |
| Pátio | portaria e coordenação do pátio | `dock_appointments` |
| Entregas | operação de rotas | `deliveries` |
| Reembolsos | SAC (pedido) e financeiro (decisão) | `refunds`, `payments` |

Regiões atendidas: `NORTH`, `SOUTH`, `EAST`, `WEST`, `CENTER`.

---

## 1. Clientes

Clientes são as empresas que pedem coletas.

| Campo | Regra |
|---|---|
| `name`, `document`, `type` | obrigatórios (**sistema**, 400) |
| `type` | `STANDARD`, `PREMIUM` ou `CORPORATE` (**sistema**). Define a prioridade quando a estratégia é `customer-type` |
| `email` | opcional |
| `active` | todo cliente nasce ativo |

**Desativação.** `PATCH /customers/{id}/deactivate` marca o cliente como inativo. Cliente inativo **não pode abrir coleta nova** (**sistema**, 400). Coletas que ele já tinha continuam seguindo o fluxo normalmente. Não existe reativação pela API.

**Lacunas**
- O documento (CNPJ/CPF) não é validado nem normalizado: o seed tem CNPJs com e sem pontuação, e nada impede dois clientes com o mesmo documento.
- Clientes e entregas não estão ligados por chave: a entrega guarda nome e documento do cliente como texto. O mesmo cliente aparece com grafias diferentes (`Livraria Página Um` / `LIVRARIA PÁGINA UM`), o que afeta relatórios e a análise de reembolsos.

---

## 2. Coletas

Uma coleta é o pedido de um cliente para a FilaFácil buscar pacotes em um endereço.

### 2.1 Abertura

`POST /pickups`. Regras (**sistema**, todas devolvem 400):

- `customerId` obrigatório, de um cliente existente e **ativo**.
- `region` obrigatória e válida.
- `address` não pode ser vazio.
- `packageCount` maior que zero.
- `manualPriority`, se informada, entre 0 e 10.

Toda coleta nasce `PENDING` e recebe uma prioridade calculada (ver 2.3).

### 2.2 Ciclo de vida

```
            ┌──────────── CANCELED ◀───────────┐
            │                ▲                 │
PENDING ────┼──▶ SCHEDULED ──┴──▶ IN_ROUTE ──▶ COLLECTED
   │        │
   └─(48h)──┴──▶ EXPIRED
```

| De | Para | Quem dispara |
|---|---|---|
| `PENDING` | `SCHEDULED` ou `CANCELED` | operação (`PATCH /pickups/{id}/status`) |
| `PENDING` | `EXPIRED` | job automático |
| `SCHEDULED` | `IN_ROUTE` ou `CANCELED` | operação |
| `IN_ROUTE` | `COLLECTED` | operação |
| `COLLECTED`, `CANCELED`, `EXPIRED` | — | estados finais |

- Qualquer transição fora da tabela devolve **409** (**sistema**). Não é possível pular etapas (ex.: `PENDING → IN_ROUTE`) nem cancelar uma coleta que já saiu (`IN_ROUTE`).
- **Envio para roteirização.** Quando a coleta entra em `IN_ROUTE` e `routing.enabled=true`, a API envia `pickupId`, `region` e `address` para o sistema de roteirização (`routing.url`, timeout de 3 s). Se o envio falhar, a mudança de status **é mantida** e só fica um aviso no log (`[ROUTE]`). Não há reenvio automático.
- **Expiração automática.** O job roda a cada `expiration.interval.minutes` (30 min; a primeira execução é 1 minuto após a API subir) e marca como `EXPIRED` toda coleta `PENDING` criada há mais de `expiration.pending.max.hours` (48 h). Coletas `SCHEDULED` não expiram.

### 2.3 Priorização

A lista de coletas (`GET /pickups`) vem ordenada por prioridade decrescente e, no empate, pela mais antiga (menor id). A prioridade depende da estratégia:

| Estratégia | Prioridade | Quando usar |
|---|---|---|
| `package-count` (padrão) | número de pacotes | maximizar volume por rota |
| `customer-type` | `CORPORATE`=3, `PREMIUM`=2, `STANDARD`=1 | honrar SLA de contrato |
| `manual` | `manualPriority` (0–10; ausente = 0) | intervenção da coordenação |

A estratégia padrão vem de `prioritization.strategy`. A listagem aceita `?strategy=` para ver a fila por outro critério sem mudar a configuração; nesse caso a prioridade é recalculada só na resposta, e o valor gravado continua sendo o calculado na criação. Filtros disponíveis: `status`, `region`, `customerType`.

### 2.4 Reagendamento

`POST /pickups/{id}/reschedule` só é permitido para coletas `CANCELED` ou `EXPIRED` (**sistema**, 409). O reagendamento **não reabre** a coleta original: cria uma **nova** coleta `PENDING` com o mesmo cliente, região, endereço, pacotes e prioridade manual, e grava `rescheduledFromId` apontando para a original. A original continua no estado final, preservando o histórico.

**Lacuna:** o reagendamento não confere se o cliente ainda está ativo.

### 2.5 Importação em lote

`POST /pickups/batch` recebe uma lista de coletas (vinda das planilhas dos parceiros) e devolve o resultado **por item**: `{index, id}` para as criadas ou `{index, error}` para as rejeitadas. Um item inválido não impede os demais.

**Lacunas.** O lote não aplica as mesmas regras da criação individual:
- não verifica se o cliente existe ou está ativo (cliente inexistente vira erro de banco no item);
- não valida `manualPriority` entre 0 e 10;
- sempre usa `packageCount` como prioridade, ignorando a estratégia configurada.

### 2.6 Comprovante de coleta

`PUT /pickups/{id}/proof` guarda o comprovante (corpo binário, não vazio) em `uploads.dir`. Um novo envio substitui a referência ao anterior. O sistema não exige comprovante para marcar `COLLECTED`.

### 2.7 Exclusão

`DELETE /pickups/{id}` remove a coleta fisicamente, em qualquer status. Para encerrar uma coleta, o caminho correto na operação é o cancelamento, que mantém o histórico. Use a exclusão apenas para corrigir lançamentos errados.

### 2.8 Resumo operacional

`GET /pickups/summary` traz o total de coletas e de pacotes, a contagem por status e por região e os contadores de eventos `PICKUP_CREATED` e `PICKUP_STATUS_CHANGED`. Os contadores ficam em memória e **zeram quando a API reinicia**.

---

## 3. Janelas de coleta

Janelas são os horários em que a FilaFácil oferece coleta aos parceiros, com capacidade máxima por janela.

- A fonte é a planilha de planejamento, exportada para `src/main/resources/pickup-windows.csv` (`id,date,start_time,end_time,max_capacity,occupied,status`). O arquivo só é lido **na inicialização**: mudar janela exige build e deploy ([runbook](runbooks/atualizacao-de-janelas.md)).
- Linhas fora do formato são ignoradas, com aviso no log.
- Status em uso: `AVAILABLE`, `FULL`, `CANCELED`. O CSV atual tem um valor fora do padrão (`lotada`), que o sistema aceita como texto livre.
- A API só consulta (`GET /pickup-windows?date=`). **Reservas são manuais**: o parceiro responde ao e-mail e o planejamento atualiza `occupied` na planilha. O sistema não liga coleta a janela e não impede ocupar além de `max_capacity`.

---

## 4. Manifestos de carga

O manifesto lista os pacotes carregados em um veículo para uma rota. É conferido **antes de cada rota sair do CD**.

- Origem: exportação do WMS em CSV (`;`) ou JSON. A API recebe só JSON: `routeCode`, `uploadedBy` e `entries[]` com `packageCode`, `route`, `vehicle`, `responsible` e `status`.
- O conteúdo é guardado integralmente, e `GET /manifests/{id}` devolve o JSON original.

Regras do checklist (**manual**, ver [runbook](runbooks/validacao-de-manifesto.md)):

1. todo pacote tem código;
2. nenhum código repetido;
3. toda linha tem rota, e é a mesma rota do manifesto;
4. toda linha tem veículo e responsável;
5. status preenchido (`LOADED` / `CARREGADO`).

**Lacuna:** a API aceita qualquer manifesto. Os exemplos em `samples/manifests` violam várias dessas regras (pacote duplicado, rota ou veículo vazios, status `PENDENTE` ou em branco, responsável com grafia diferente) e mesmo assim seriam aceitos.

---

## 5. Agendamento de docas

Transportadoras parceiras agendam horário em uma doca do CD para descarregar.

- Obrigatórios: `carrier`, `truckPlate`, `dock`, `date`, `startTime` (**sistema**, 400).
- `endTime` é opcional; se não vier, o agendamento dura **1 hora**.
- A placa é gravada em maiúsculas; todo agendamento nasce `SCHEDULED`.
- Anexos (nota fiscal, comprovante) vão por `PUT /dock-appointments/{id}/attachment` com o nome no header `X-File-Name`, e ficam em `uploads/docks/`. O anexo não tem backup.

Regras **manuais** ([runbook](runbooks/agendamento-de-docas.md)):

- **Uma doca não pode ter dois agendamentos sobrepostos.** A portaria confere na planilha antes de registrar. O sistema não verifica, e o seed já tem um conflito (agendamentos 1 e 2, doca 1, 05/10).
- Cancelamento e troca de horário são feitos só na planilha; a API não tem essas operações.

**Lacuna:** placas gravadas antes da normalização podem estar em minúsculas (`rty7u89` no seed).

---

## 6. Entregas e ocorrências

Entregas foram importadas das planilhas da operação e, por isso, guardam cliente e motorista como texto, sem chave para `customers`.

- Status usados: `IN_TRANSIT`, `DELIVERED`, `DELAYED`, `RETURNED`. Há registros com valores fora do padrão (ex.: `entregue`); o filtro por status ignora maiúsculas/minúsculas, mas não trata sinônimos.
- A API é somente leitura para dados da entrega. Filtros: `status`, `driver` e `customer` (busca parcial), período por `scheduledDate` (`from`/`to`) e `hasRefund`. Paginação com `page` (começa em 1) e `size` (padrão 50).

**Ocorrências.** `POST /deliveries/{id}/occurrences` registra um problema na entrega (`type` obrigatório, gravado em maiúsculas, mais `notes` opcional). Tipos em uso: `DELAY`, `CUSTOMER_ABSENT`, `WRONG_ADDRESS`, `DAMAGED_PACKAGE`, `INCOMPLETE_DELIVERY`.

**Lacunas**
- A entrega guarda **só a última ocorrência**: registrar outra sobrescreve a anterior, e o histórico se perde.
- O tipo é texto livre; a API não restringe aos valores acima.

---

## 7. Reembolsos

Reembolso é a devolução de parte ou de todo o frete ao cliente por falha na entrega.

### 7.1 Pedido (SAC)

`POST /refunds` (**sistema**, 400):

- `deliveryId` obrigatório;
- `reason` deve ser `DELAY`, `DAMAGED_PACKAGE` ou `INCOMPLETE_DELIVERY`;
- `amount` maior que zero.

O pedido nasce `REQUESTED`. **Lacuna:** a API não verifica se a entrega existe, nem se o valor cabe no frete. O seed tem reembolsos para entregas inexistentes (9999 e 1500).

### 7.2 Decisão (financeiro)

```
REQUESTED ──approve──▶ APPROVED (+ pagamento)
    └──────reject────▶ REJECTED
```

- Só pedidos `REQUESTED` podem ser decididos; caso contrário, **422** (**sistema**).
- **Teto do reembolso:** a soma do que já foi reembolsado na entrega com o novo valor não pode passar do frete (`refunded_amount + amount ≤ freight_value`). Violar devolve **422**.
- Na aprovação, o sistema grava quem aprovou e quando, cria um registro em `payments` com o valor e soma o valor em `refunded_amount` da entrega.
- Na recusa, grava quem recusou, quando e o motivo (`notes`). O campo de responsável é o mesmo da aprovação (`approved_by`).

**Lacuna crítica:** a aprovação não roda em transação, e o status muda para `APPROVED` **antes** de validar a entrega e o teto. Quando a validação falha (404/422), o reembolso fica `APPROVED` sem pagamento. O [runbook](runbooks/aprovacao-de-reembolso.md) descreve como detectar e corrigir esse caso com o DBA.

Regras **manuais** do financeiro: levantar o histórico da entrega, do cliente e do motorista antes de decidir, e registrar decisão, valor, responsável e motivo na planilha de análise, porque o sistema não guarda trilha de auditoria.

### 7.3 Relatório

`GET /refunds/report` consolida:

- total, contagem por status e por motivo;
- valor aprovado total;
- taxa de aprovação = aprovados ÷ (aprovados + recusados), com uma casa decimal; pedidos pendentes não entram;
- valor aprovado por motorista e por cliente (agrupado pelo nome como está na entrega, então grafias diferentes aparecem separadas);
- `refundsWithoutDelivery`: pedidos cuja entrega não existe.

---

## 8. Resumo do que é validado

| Regra | Sistema | Manual |
|---|:---:|:---:|
| Cliente inativo não abre coleta (criação individual) | ✅ | |
| Cliente inativo não abre coleta (lote / reagendamento) | | ⚠️ não coberto |
| Transições de status da coleta | ✅ | |
| Expiração de coleta pendente após 48 h | ✅ | |
| Reagendar só cancelada/expirada | ✅ | |
| Capacidade da janela de coleta | | ✅ planilha |
| Conferência do manifesto | | ✅ checklist |
| Conflito de horário na doca | | ✅ planilha |
| Reembolso dentro do frete | ✅ (na aprovação) | ✅ |
| Entrega do reembolso existe | ✅ (na aprovação) | |
| Histórico de ocorrências | | ⚠️ perdido |
| Auditoria de decisões de reembolso | | ✅ planilha |
