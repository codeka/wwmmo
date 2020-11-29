@ECHO OFF
SET SCRIPTPATH=%~dp0

rem no idea how to really do this...
PUSHD %SCRIPTPATH%
CD ..
SET ROOTPATH=%CD%
POPD

SET DISTPATH=%ROOTPATH%\server\build\distributions\server.zip

PUSHD %ROOTPATH%
CALL gradlew.bat --daemon :server:distZip
POPD

rem copy the file from the build location to the server
scp %DISTPATH% wwmmo@game.war-worlds.com:/home/wwmmo

