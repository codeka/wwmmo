
import common
import os
import pathlib
import signal
import subprocess
import time

dataPath = pathlib.Path(common.rootPath, 'server/src/main/data')

def build_and_run_server():
    os.system("adb reverse tcp:8080 tcp:8080")
    os.system("adb reverse tcp:8081 tcp:8081")

    err = os.system("cd " + str(common.rootPath) + " && " + common.gradleCmd +
                    " --daemon :server:installDist -Pandroid.debug.obsoleteApi=true")
    if err != 0:
        return err

    classpath = ""
    for jar in pathlib.Path(common.installPath, "lib").glob("*.jar"):
        if classpath != "":
            classpath += os.pathsep
        classpath += str(jar)

    # TODO: If we keep getting 'is neither empty nor does it contain an installation' errors, we
    # should manually create the bin/ and lib/ folder in the installation directory first to trick
    # the build into thinking it's a valid installation...

    cmd = [
        "java",
        "-classpath", classpath,
        "-DConfigFile=" + str(dataPath) + "/config-debug.json",
        "au.com.codeka.warworlds.server.Program"
        ]

    # Note: running the server via the batch file is kind of annoying on Windows due to the way
    # it does dumb stuff on Ctrl+C. So we set our classpath and run the java server directly
    # instead.
    try:
      proc = subprocess.Popen(cmd, cwd=str(dataPath.parent))
      proc.wait()
    except KeyboardInterrupt:
      proc.send_signal(signal.CTRL_C_EVENT)
      proc.wait()

    return proc.returncode

while True:
    returncode = build_and_run_server()
    try:
        time.sleep(1) # Give it a second to finish printing stdout
        print("War Worlds exited prematurely (" + str(returncode) + "), waiting 2 seconds, then restarting.")
        time.sleep(2)
    except KeyboardInterrupt:
        print("Sleep interrupted, exiting.")
        break

