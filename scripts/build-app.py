import common
import os
import pathlib
import shutil
import subprocess

# Do the build
os.system("cd " + str(common.rootPath) + " && " + common.gradleCmd + " --daemon :client:bundleRelease")

# Try to grab the version code from the manifest
bundleTool = pathlib.Path(common.rootPath, "bundletool-all.jar")
cmd = ("java -jar " + str(bundleTool) + " dump manifest --bundle " + str(common.aabPath) +
       " --xpath /manifest/@android:versionCode")
versionCode = subprocess.check_output(cmd, shell=True)
versionCode = versionCode.decode("utf-8").strip()

destPath = pathlib.Path(common.rootPath, "../deploy/client/wwmmo-1.0." + versionCode + ".aab")
shutil.copyfile(common.aabPath, destPath)

print("built bundle: " + str(destPath.resolve()))

# If you've built the apks, delete it now since bundletool complains if the file exists
apksPath = pathlib.Path(common.rootPath, "../deploy/wwmmo.apks")
if os.path.exists(apksPath):
  os.remove(apksPath)

print("Install with:")
print("  java -jar " + str(bundleTool) + " build-apks --connected-device --bundle=" +
      str(destPath.resolve()) + " --output " + str(apksPath.resolve()) + " --ks " +
      str(pathlib.Path(common.rootPath, "../keystore.jks")) +
      " --ks-key-alias Codeka && java -jar " + str(bundleTool) + " install-apks --apks " +
      str(apksPath))
