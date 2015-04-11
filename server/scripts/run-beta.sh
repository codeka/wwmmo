#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd $DIR
SERVER_OPTS=""
SERVER_OPTS="$SERVER_OPTS -Dau.com.codeka.warworlds.server.ConfigFile=data/config-beta.json"
SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.config.file=logging-beta.properties"
SERVER_OPTS="$SERVER_OPTS" exec ./bin/server $*
popd

