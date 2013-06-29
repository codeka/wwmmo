#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd $DIR
java -cp "bin/*" \
     -Dau.com.codeka.warworlds.server.basePath=$DIR/bin/ \
     -Dau.com.codeka.warworlds.server.listenPort=8080 \
     -Dau.com.codeka.warworlds.server.dbName=wwmmo \
     -Dau.com.codeka.warworlds.server.dbUser=wwmmo_user \
    '-Dau.com.codeka.warworlds.server.dbPass=H98765gf!s876#Hdf2%7f' \
     -Dau.com.codeka.warworlds.server.realmName=Beta \
     -Djava.util.logging.config.file=logging-beta.properties \
     au.com.codeka.warworlds.server.Runner $*
popd
