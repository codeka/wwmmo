@ECHO OFF
SET SCRIPTPATH=%~dp0

rem no idea how to really do this...
PUSHD %SCRIPTPATH%
CD ..
SET ROOTPATH=%CD%
POPD

SET APKPATH=%ROOTPATH%\client\build\outputs\bundle\release\client-release.aab

PUSHD %ROOTPATH%
CALL gradlew.bat --daemon :client:bundleRelease
POPD

COPY %APKPATH% %ROOTPATH%\..\deploy\client\wwmmo-0.9.9999.aab

