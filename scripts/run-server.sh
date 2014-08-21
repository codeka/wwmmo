#!/bin/bash

pushd code/common
ant build
popd
pushd code/server
ant package
RETVAL=$?
popd

[ $RETVAL -ne 0 ] && exit

pushd deploy
./run-debug.sh $*
popd
