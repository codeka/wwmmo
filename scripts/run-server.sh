#!/bin/bash

set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
INSTALLPATH=$ROOTPATH/server/build/install/server

adb reverse tcp:8080 tcp:8080 || true

function build_and_run_server()
{
  pushd $ROOTPATH > /dev/null
  ./gradlew --daemon :server:installDist
  popd > /dev/null

  pushd $INSTALLPATH > /dev/null
  SERVER_OPTS=""
  SERVER_OPTS="$SERVER_OPTS -Dau.com.codeka.warworlds.server.ConfigFile=$INSTALLPATH/data/config-debug.json"
  SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.config.file=logging-debug.properties"
  SERVER_OPTS="$SERVER_OPTS" ./bin/server $*
  SERVER_STATUS=$?
  popd > /dev/null
  return $SERVER_STATUS
}

until build_and_run_server; do
  echo "War Worlds exited prematurely with exit code $?. Restarting.." >&2
  sleep 1
done
echo "War Worlds exited normally." >&2

