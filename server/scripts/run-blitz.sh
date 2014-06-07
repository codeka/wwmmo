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
     -Dau.com.codeka.warworlds.server.basePath=$DIR/bin/ \
     -Dau.com.codeka.warworlds.server.listenPort=8081 \
     -Dau.com.codeka.warworlds.server.dbName=wwmmo \
     -Dau.com.codeka.warworlds.server.dbUser=wwmmo_user \
    '-Dau.com.codeka.warworlds.server.dbPass=H98765gf!s876#Hdf2%7f' \
     -Dau.com.codeka.warworlds.server.dbSchema=blitz \
     -Dau.com.codeka.warworlds.server.realmName=Blitz \
     -Djava.util.logging.config.file=logging-blitz.properties \
     au.com.codeka.warworlds.server.Runner $@ &

echo "$!" > $PIDFILE

popd
