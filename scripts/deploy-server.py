
import common
import os
import pathlib
import shutil
import subprocess

# Do the build
os.system("cd " + str(common.rootPath) + " && " + common.gradleCmd + " --daemon :server:distZip")

# scp it to the server
os.system("cd " + str(common.rootPath) + " && scp " + str(common.distPath) + " wwmmo@game.war-worlds.com:/home/wwmmo")
