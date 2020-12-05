
import common
import os
import pathlib
import shutil
import subprocess

# Do the build
os.system("cd " + str(common.rootPath) + " && " + common.gradleCmd + " --daemon :client:bundleRelease")

# Try to grab the version code from the manifest
bundleTool = pathlib.Path(common.rootPath, "bundletool-all.jar")
cmd = "java -jar " + str(bundleTool) + " dump manifest --bundle " + str(common.aabPath) + " --xpath /manifest/@android:versionCode"
versionCode = subprocess.check_output(cmd, shell=True)
versionCode = versionCode.decode("utf-8").strip()

destPath = pathlib.Path(common.rootPath, "../deploy/client/wwmmo-1.0." + versionCode + ".aab")
shutil.copyfile(common.aabPath, destPath)
