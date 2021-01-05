# This file should be copied to the prod server's root directory, and run with:
#   cd server/live
#   python3 ~/prod-run-server.py

import os
import pathlib
import signal
import subprocess

path = pathlib.Path(os.getcwd())

classpath = ""
for jar in pathlib.Path(path, "lib").glob("*.jar"):
    if classpath != "":
        classpath += os.pathsep
    classpath += str(jar)

# TODO: If we keep getting 'is neither empty nor does it contain an installation' errors, we
# should manually create the bin/ and lib/ folder in the installation directory first to trick
# the build into thinking it's a valid installation...

cmd = [
    "java",
    "-classpath", classpath,
    "-DConfigFile=" + str(path) + "/data/config-prod.json",
    "-Djava.util.logging.config.file=logging.properties",
    "au.com.codeka.warworlds.server.Program"
]

# Note: running the server via the batch file is kind of annoying on Windows due to the way
# it does dumb stuff on Ctrl+C. So we set our classpath and run the java server directly
# instead.
try:
    proc = subprocess.Popen(cmd, cwd=str(path))
    proc.wait()
except KeyboardInterrupt:
    proc.send_signal(signal.CTRL_C_EVENT)
    proc.wait()
