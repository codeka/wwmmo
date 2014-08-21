#!/bin/bash

export CLASSPATH=/usr/share/java/ant-contrib.jar
export ANDROID_HOME=$HOME/android-sdk

pushd $HOME/src/wwmmo/code
cd common && ant clean build
cd ../planet-render && ant clean build
cd ../control-field && ant clean build
cd ../client && ant clean release -Dndk=false
cd ../common && ant clean
cd ../planet-render && ant clean
cd ../control-field && ant clean
cd ../client && ant clean
popd

