#!/bin/bash

pushd /home/dean/software/wwmmo/code
cd common && ant clean
cd ../planet-render && ant clean
cd ../control-field && ant clean
cd ../client && ant clean -Dndk=false
cd ../server && ant clean
popd

