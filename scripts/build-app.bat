@ECHO OFF
SET SCRIPTPATH=%~dp0

rem no idea how to really do this...
PUSHD %SCRIPTPATH%
CD ..
SET ROOTPATH=%CD%
POPD

PUSHD %ROOTPATH%
CALL gradlew.bat --daemon :client:deploy
POPD
