# Agendamento de docas de descarga

Responsável: portaria e coordenação do pátio.

1. A transportadora manda mensagem (WhatsApp da portaria) pedindo doca, data e horário, com a placa do caminhão.
2. A portaria confere na planilha "Docas" se o horário está livre **naquela doca**.
3. Registrar no sistema:
   ```bash
   curl -X POST -H 'Content-Type: application/json' \
     -d '{"carrier":"...","truckPlate":"...","dock":1,"date":"2026-10-05","startTime":"08:00"}' \
     http://<servidor>:8080/dock-appointments
   ```
   O sistema **não verifica conflito**: a conferência do passo 2 é obrigatória.
4. Se a transportadora mandar nota fiscal ou comprovante, anexar:
   ```bash
   curl -X PUT -H 'X-File-Name: nf.pdf' --data-binary @nf.pdf \
     http://<servidor>:8080/dock-appointments/<id>/attachment
   ```
   Os arquivos ficam no disco do servidor, em `uploads/docks/`. **Não apagar essa pasta**, porque não há cópia.
5. Cancelamentos e trocas de horário: ajustar só na planilha (o sistema ainda não permite) e avisar o pátio por rádio.

Para ver os agendamentos do dia, filtrar na planilha. O sistema lista todos os agendamentos de uma vez.
