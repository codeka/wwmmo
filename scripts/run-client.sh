#!/bin/bash

export CLASSPATH=/usr/share/java/ant-contrib.jar
export ANDROID_HOME=/home/dean/android-sdk

pushd /home/dean/software/wwmmo/code
cd common && ant build
cd ../planet-render && ant build
cd ../control-field && ant build
cd ../client && ant debug -Dndk=false
cd ..
adb install -r client/bin/warworlds.apk
adb shell am start -n au.com.codeka.warworlds/au.com.codeka.warworlds.WarWorldsActivity
popd

