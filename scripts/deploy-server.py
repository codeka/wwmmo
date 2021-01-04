
import common
import os

# Do the build
os.system("cd " + str(common.rootPath) + " && " + common.gradleCmd + " --daemon :server:distZip")

# scp it to the server
os.system("cd " + str(common.rootPath) + " && scp " + str(common.distPath) + " warworld@game2.war-worlds.com:/home/warworld")
