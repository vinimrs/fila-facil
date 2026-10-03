# Atualização das janelas de coleta

Responsável: planejamento de coletas. Frequência: toda sexta, para as próximas duas semanas (ou quando um parceiro pede mudança).

1. Abrir a planilha de janelas compartilhada e ajustar capacidades, ocupação e status.
2. Exportar como CSV no formato `id,date,start_time,end_time,max_capacity,occupied,status`
   (data `AAAA-MM-DD`; linhas com outro formato são ignoradas pelo sistema).
3. Pedir ao time de backend para substituir `src/main/resources/pickup-windows.csv`, gerar o build e fazer o deploy
   (ver `deploy-manual.md`). O sistema só lê o arquivo na inicialização.
4. Enquanto o deploy não acontece, enviar a planilha por e-mail para as empresas parceiras com o assunto
   `Janelas de coleta - semana XX`.
5. Quando um parceiro responder pedindo um horário, atualizar a coluna `occupied` na planilha
   (o sistema não registra reservas).

Atenção: parceiros costumam responder em cima de versões antigas da planilha. Sempre conferir a data do anexo.
