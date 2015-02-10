#!/bin/bash

set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
INSTALLPATH=$ROOTPATH/intel/generator/build/install/generator

pushd $ROOTPATH > /dev/null
./gradlew --daemon :intel:generator:installApp
popd > /dev/null

pushd $INSTALLPATH > /dev/null
./bin/generator $*
popd > /dev/null


