#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

PIDFILE=""

OPTIND=1
while getopts "p:" opt; do
  case "$opt" in
    p)
      PIDFILE=$OPTARG
      ;;
  esac
done
shift "$((OPTIND-1))"

pushd $DIR
SERVER_OPTS=""
SERVER_OPTS="$SERVER_OPTS -Dau.com.codeka.warworlds.server.ConfigFile=data/config-blitz.json"
SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.config.file=logging-blitz.properties"
SERVER_OPTS="$SERVER_OPTS" nohup ./bin/server $* &
echo "$!" > $PIDFILE
popd
