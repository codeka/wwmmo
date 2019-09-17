#!/bin/bash


set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
APKPATH=$ROOTPATH/client/build/outputs/apk/release/client-release.apk

pushd $ROOTPATH > /dev/null
./gradlew --daemon :client:assembleRelease $@
popd > /dev/null

# Assumes ANDROID_HOME is set to your Android SDK directory
ANDROID_HOME=/home/dean/android/sdk
BUILDTOOLS_DIR=`ls $ANDROID_HOME/build-tools/ | sort -V | tail -1`
VERSIONCODE=`$ANDROID_HOME/build-tools/$BUILDTOOLS_DIR/aapt dump badging $APKPATH | egrep -o "versionCode='[0-9]+'" | egrep -o "[0-9]+"`

cp $APKPATH $ROOTPATH/../apk/warworlds-$VERSIONCODE.apk

