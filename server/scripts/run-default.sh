#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

function run_server()
{
  pushd $DIR > /dev/null
  SERVER_OPTS=""
  SERVER_OPTS="$SERVER_OPTS -Djava.awt.headless=true"
  SERVER_OPTS="$SERVER_OPTS -Dau.com.codeka.warworlds.server.ConfigFile=data/config-default.json"
  SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.config.file=logging-default.properties"
  SERVER_OPTS="$SERVER_OPTS" ./bin/server $*
  SERVER_STATUS=$?
  popd > /dev/null
  return $SERVER_STATUS
}

until run_server; do
  echo "War Worlds exited prematurely with exit code $?. Restarting.." >&2
  sleep 1
done
echo "War Worlds exited normally." >&2

