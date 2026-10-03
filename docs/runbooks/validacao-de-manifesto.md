# Validação de manifesto de carga

Responsável: analista de expedição do CD. Feito **antes de cada rota sair**.

O WMS exporta o manifesto em CSV (separado por `;`) ou JSON. Exemplos em `samples/manifests/`.

## Checklist (manual)
Abrir o arquivo no Excel e conferir linha a linha:

- [ ] Todo pacote tem código preenchido.
- [ ] Nenhum código de pacote aparece duas vezes (usar *Formatação condicional → Valores duplicados*).
- [ ] Todas as linhas têm rota, e é a mesma rota do nome do arquivo.
- [ ] Todas as linhas têm veículo (placa) e responsável (motorista).
- [ ] Status preenchido (`LOADED`/`CARREGADO`).

Se algum item falhar: corrigir no WMS, exportar de novo e recomeçar a conferência.

## Envio
Se estiver tudo certo, converter o CSV para o JSON do sistema (modelo em `samples/manifests/*.json`) e enviar:

```bash
curl -X POST -H 'Content-Type: application/json' \
  --data-binary @manifesto.json http://<servidor>:8080/manifests
```

O sistema aceita qualquer manifesto: **a conferência acima é a única validação**.
Anotar na planilha "Controle de Manifestos" quem validou e o horário.
