#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd $DIR
java -cp "bin/*" \
     -Dau.com.codeka.warworlds.server.basePath=$DIR/bin/ \
     -Djava.util.logging.config.file=logging.properties \
     au.com.codeka.warworlds.server.Runner $*
popd
