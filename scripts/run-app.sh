#!/bin/bash


set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
APKPATH=$ROOTPATH/client/build/outputs/apk/client-debug.apk

pushd $ROOTPATH > /dev/null
./gradlew --daemon :client:assembleDebug $@
popd > /dev/null

# Assumes ANDROID_HOME is set to your Android SDK directory
BUILDTOOLS_DIR=`ls $ANDROID_HOME/build-tools/ | sort -V | tail -1`

$ANDROID_HOME/platform-tools/adb install -r $APKPATH


