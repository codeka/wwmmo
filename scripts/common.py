
import pathlib
import platform

rootPath = pathlib.Path(__file__).parent.parent.absolute()
aabPath = pathlib.Path(rootPath, 'client/build/outputs/bundle/release/client-release.aab')
apkPath = pathlib.Path(rootPath, 'client/build/outputs/apk/release/client-release.apk')
distPath = pathlib.Path(rootPath, 'server/build/distributions/server.zip')
installPath = pathlib.Path(rootPath, 'server/build/install/server')

gradleCmd = './gradlew'
if platform.system() == 'Windows':
  gradleCmd = r".\gradlew.bat"
