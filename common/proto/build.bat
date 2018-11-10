@echo off
protoc -I=. --java_out=..\src *.proto

