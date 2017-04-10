#!/bin/bash

set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
INSTALLPATH=$ROOTPATH/server/build/install/server
RUNPATH=$ROOTPATH/server/src/main

pushd $ROOTPATH > /dev/null
./gradlew --daemon :server:installDist
popd > /dev/null

pushd $RUNPATH > /dev/null
SERVER_OPTS=""
SERVER_OPTS="$SERVER_OPTS -DConfigFile=$RUNPATH/data/config-debug.json"
SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.config.file=$RUNPATH/logging.properties"
SERVER_OPTS="$SERVER_OPTS" exec $INSTALLPATH/bin/server $*
popd > /dev/null


