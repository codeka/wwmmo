#!/bin/bash


set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
AABPATH=$ROOTPATH/client/build/outputs/bundle/release/client-release.aab

pushd $ROOTPATH > /dev/null
./gradlew --daemon :client:bundleRelease $@
popd > /dev/null

# Note: to update bundletool, download it from https://github.com/google/bundletool/releases
VERSIONCODE=`java -jar $ROOTPATH/bundletool-all.jar dump manifest \
    --bundle $AABPATH --xpath=/manifest/@android:versionCode`

cp $AABPATH $ROOTPATH/../apk/warworlds-$VERSIONCODE.aab

