# Aprovação de reembolso

Responsável: financeiro. Pedidos chegam pelo SAC (atraso, pacote danificado ou entrega incompleta).

## 1. Levantar o histórico (manual)
O sistema não mostra tudo junto, então rodamos estas consultas no banco (acesso de leitura com o DBA):

```sql
-- dados da entrega, cliente e motorista
SELECT * FROM deliveries WHERE tracking_code = 'FF2026000XX';

-- outros reembolsos da mesma entrega
SELECT * FROM refunds WHERE delivery_id = <id>;

-- outros reembolsos do mesmo cliente (o nome pode estar escrito de jeitos diferentes!)
SELECT r.*, d.customer_name
FROM refunds r, deliveries d
WHERE r.delivery_id = d.id AND d.customer_name ILIKE '%acme%';

-- histórico do motorista
SELECT occurrence_type, count(*) FROM deliveries WHERE driver_name ILIKE '%silva%' GROUP BY 1;
```

Copiar os resultados para a planilha de análise do dia.

## 2. Decidir
- Valor do reembolso não pode passar do frete (`freight_value - refunded_amount`).
- A ocorrência registrada é só a última, e as anteriores se perdem. Na dúvida, perguntar ao SAC.

## 3. Aprovar no sistema
```bash
curl -X POST -H 'Content-Type: application/json' \
  -d '{"approvedBy":"seu.usuario"}' http://<servidor>:8080/refunds/<id>/approve
```

Se a API devolver erro, **conferir no banco** se o reembolso ficou `APPROVED` sem pagamento:

```sql
SELECT r.id, r.status, p.id AS payment_id
FROM refunds r LEFT JOIN payments p ON p.refund_id = r.id
WHERE r.id = <id>;
```

Se ficou, pedir ao DBA para voltar o status para `REQUESTED` na mão.

## 4. Registro
Não existe histórico de quem alterou o quê no sistema. Anotar na planilha: id do reembolso, decisão, valor, quem aprovou e o motivo.
