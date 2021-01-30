
import common
import os

# TODO: check that JDK is not > 11, which iwhat the server expects

# Do the build
os.system("cd " + str(common.rootPath) + " && " + common.gradleCmd + " --daemon :server:distZip")

# scp it to the server
os.system("cd " + str(common.rootPath) + " && scp " + str(common.distPath) + " wwmmo@game.war-worlds.com:/home/wwmmo")
