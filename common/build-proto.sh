#!/bin/bash

java -jar libs/wire-compiler-2.1.2-jar-with-dependencies.jar --proto_path=src/main/proto --java_out=src/main/java empire.proto star.proto packets.proto account.proto
