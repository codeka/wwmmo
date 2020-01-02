#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function run_server()
{
  pushd $DIR
  SERVER_OPTS=""
  SERVER_OPTS="$SERVER_OPTS -Djava.awt.headless=true"
  SERVER_OPTS="$SERVER_OPTS -Dau.com.codeka.warworlds.server.ConfigFile=data/config-default.json"
  SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.config.file=logging-default.properties"
  SERVER_OPTS="$SERVER_OPTS" exec ./bin/server $*
  popd
}

until run_server; do
  echo "War Worlds exited prematurely with exit code $?. Restarting.." >&2
  sleep 1
done


