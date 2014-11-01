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
nohup java -cp "bin/*" \
     -Dau.com.codeka.warworlds.server.ConfigFile=$DIR/data/config-blitz.json \
     -Djava.util.logging.config.file=logging-blitz.properties \
     au.com.codeka.warworlds.server.Runner $@ &

echo "$!" > $PIDFILE

popd
