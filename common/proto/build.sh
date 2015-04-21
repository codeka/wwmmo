#!/bin/bash
#
# Builds the .proto files and generates the required java/python code.
#

java -jar wire-compiler-1.8.0-SNAPSHOT-jar-with-dependencies.jar \
     --proto_path=. --java_out=../src *.proto

