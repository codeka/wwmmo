#!/bin/bash

pushd code/common
ant clean build
cd ../server
ant clean deploy
ant clean
popd

