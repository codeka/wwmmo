#!/bin/bash
#
# Builds the .proto files and generates the required java/python code.
#

protoc -I=. --python_out=. --java_out=../../WarWorlds-ClientCommon/gen *.proto

