#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd $DIR
SERVER_OPTS=""
SERVER_OPTS="$SERVER_OPTS -Dau.com.codeka.warworlds.server.ConfigFile=data/config-blitz.json"
SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.config.file=logging-blitz.properties"
SERVER_OPTS="$SERVER_OPTS" ./bin/server $*
popd
