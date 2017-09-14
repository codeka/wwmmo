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

# This is so the app running on the phone will be able to connect to us. 8080 for the normal
# HTTP stuff, 8081 is our custom port for the long-lived connection. It's OK if these fail, usually
# it means you don't have a device connected yet.
adb reverse tcp:8080 tcp:8080 || true
adb reverse tcp:8081 tcp:8081 || true

pushd $RUNPATH > /dev/null
SERVER_OPTS=""
SERVER_OPTS="$SERVER_OPTS -DConfigFile=$RUNPATH/data/config-debug.json"
SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.config.file=$RUNPATH/logging.properties"
SERVER_OPTS="$SERVER_OPTS" exec $INSTALLPATH/bin/server $*
popd > /dev/null


