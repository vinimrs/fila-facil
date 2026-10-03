#!/usr/bin/env bash
# Deploy do FilaFacil no servidor de producao.
# Uso: ./scripts/deploy.sh
set -e

SERVER=deploy@10.20.0.15
APP_DIR=/opt/filafacil

echo ">> build"
./gradlew clean installDist -x test

echo ">> copiando para $SERVER"
scp -r build/install/filafacil/* $SERVER:$APP_DIR/

echo ">> reiniciando"
ssh $SERVER "pkill -f filafacil || true; cd $APP_DIR && nohup bin/filafacil > app.log 2>&1 &"

echo ">> deploy feito. Conferir se a API subiu (ver docs/runbooks/deploy-manual.md)"
