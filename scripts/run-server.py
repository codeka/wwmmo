
import common
import os
import pathlib
import signal
import subprocess
import time

def build_and_run_server():
    os.system("adb reverse tcp:8080 tcp:8080")

    os.system("cd " + str(common.rootPath) + " && " + common.gradleCmd + " --daemon :server:installDist")

    classpath = ""
    for jar in pathlib.Path(common.installPath, "lib").glob("*.jar"):
        if classpath != "":
            classpath += os.pathsep
        classpath += str(jar)

    cmd = [
        "java",
        "-classpath", classpath,
        "-Dau.com.codeka.warworlds.server.ConfigFile=" + str(common.installPath) + "/data/config-debug.json",
        "-Djava.util.logging.config.file=logging-debug.properties",
        "au.com.codeka.warworlds.server.Runner"
        ]


    # Note: running the server via the batch file is kind of annoying on Windows due to the way
    # it does dumb stuff on Ctrl+C. So we set our classpath and run the java server directly
    # instead.
    try:
      proc = subprocess.Popen(cmd, cwd=str(common.installPath))
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

