#!/bin/bash

set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
INSTALLPATH=$ROOTPATH/intel/build/install/intel

pushd $ROOTPATH > /dev/null
./gradlew --daemon :intel:installApp
popd > /dev/null

pushd $INSTALLPATH > /dev/null
INTEL_OPTS=""
INTEL_OPTS="$INTEL_OPTS -Dau.com.codeka.warworlds.intel.ConfigFile=$INSTALLPATH/data/config-debug.json"
INTEL_OPTS="$INTEL_OPTS -Djava.util.logging.config.file=logging-debug.properties"
INTEL_OPTS="$INTEL_OPTS" exec ./bin/intel $*
popd > /dev/null


