# Deploy em produção

Responsável: time de backend. Janela combinada: terça e quinta, depois das 19h.

## Antes
1. Avisar no grupo da operação que a API vai ficar fora por alguns minutos.
2. Conferir se o `application.properties` está com os dados de produção (banco `10.20.0.30`, senha do cofre).
   Em dev ele aponta para `localhost`, então **lembrar de trocar antes do build e destrocar depois**.
3. Garantir que você tem a chave SSH do usuário `deploy`.

## Deploy
```bash
./scripts/deploy.sh
```
O script pula os testes porque eles demoram (sobem um Postgres em container).

## Depois
1. Entrar no servidor: `ssh deploy@10.20.0.15`
2. `tail -f /opt/filafacil/app.log` até aparecer `Server started on port 8080`.
3. Testar no navegador: `http://10.20.0.15:8080/customers` deve devolver a lista.
4. Avisar no grupo que voltou.

## Se der problema
Não existe versão anterior guardada no servidor. Fazer `git checkout` do commit anterior, gerar o build de novo e rodar o script.
Se a migration do Flyway já tiver rodado, falar com o DBA antes.
