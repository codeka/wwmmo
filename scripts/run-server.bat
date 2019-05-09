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
CALL gradlew.bat --daemon :server:installDist -Pandroid.debug.obsoleteApi=true
POPD

rem This is so the app running on the phone will be able to connect to us. 8080 for the normal
rem HTTP stuff, 8081 is our custom port for the long-lived connection. It's OK if these fail, usually
rem it means you don't have a device connected yet.
adb reverse tcp:8080 tcp:8080
adb reverse tcp:8081 tcp:8081

pushd %RUNPATH%
SET DEFAULT_JVM_OPTS=
SET JAVA_OPTS=
SET SERVER_OPTS=
SET SERVER_OPTS=-DConfigFile=%RUNPATH%\data\config-debug.json
SET SERVER_OPTS=%SERVER_OPTS% -Djava.util.logging.config.file=%RUNPATH%\logging.properties
CALL %INSTALLPATH%\bin\server.bat %*
POPD
