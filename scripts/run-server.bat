@ECHO OFF
SET SCRIPTPATH=%~dp0

rem no idea how to really do this...
PUSHD %SCRIPTPATH%
CD ..
SET ROOTPATH=%CD%
POPD

SET INSTALLPATH=%ROOTPATH%\server\build\install\server
SET RUNPATH=%ROOTPATH%\server\src\main

PUSHD %ROOTPATH%
CALL gradlew.bat --daemon :server:installDist
POPD

pushd %RUNPATH%
SET DEFAULT_JVM_OPTS=
SET JAVA_OPTS=
SET SERVER_OPTS=
SET SERVER_OPTS=-Dau.com.codeka.warworlds.server.ConfigFile=%INSTALLPATH%\data\config-debug.json
SET SERVER_OPTS=%SERVER_OPTS% -Djava.util.logging.config.file=%INSTALLPATH%\logging-debug.properties
CALL %INSTALLPATH%\bin\server.bat %*
POPD
