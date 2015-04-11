#!/bin/bash

trap "echo hello" DEBUG

set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
INSTALLPATH=$ROOTPATH/server/build/install/server

pushd $ROOTPATH > /dev/null
./gradlew --daemon :server:installApp
popd > /dev/null

#trap "trap - SIGTERM && kill -- -$$" SIGINT SIGTERM EXIT

pushd $INSTALLPATH > /dev/null
SERVER_OPTS=""
SERVER_OPTS="$SERVER_OPTS -Dau.com.codeka.warworlds.server.ConfigFile=$INSTALLPATH/data/config-debug.json"
SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.config.file=logging-debug.properties"
SERVER_OPTS="$SERVER_OPTS" exec ./bin/server $*
popd > /dev/null


