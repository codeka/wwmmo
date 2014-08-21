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
chmod +x ./run-debug.sh
./run-debug.sh $*
popd
