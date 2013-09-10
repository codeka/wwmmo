#!/bin/bash
#
# Builds the .proto files and generates the required java/python code.
#

protoc -I=. --java_out=../src *.proto

