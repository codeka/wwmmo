@ECHO OFF
SET SCRIPTPATH=%~dp0

rem no idea how to really do this...
PUSHD %SCRIPTPATH%
CD ..
SET ROOTPATH=%CD%
POPD

SET APKPATH=%ROOTPATH%\client\build\outputs\apk\release\client-release.apk

PUSHD %ROOTPATH%
CALL gradlew.bat --daemon :client:assembleRelease
POPD

COPY %APKPATH% %ROOTPATH%\..\deploy\client\wwmmo-0.9.9999.apk

