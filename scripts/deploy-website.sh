#!/bin/bash

WEBSITE_DIR=/home/dean/software/wwmmo/code/website

# update the RESOURCE_VERSION in the file handlers/__init__.py so that
# all our resource references go to a different file. This is our cache-busting
# mechanism.
pushd $WEBSITE_DIR
sed -r 's/(RESOURCE_VERSION\s*=\s*)([0-9]+)/echo \1$((\2+1))/ge' handlers/__init__.py > /tmp/wwmmo.txt
mv /tmp/wwmmo.txt handlers/__init__.py
popd

# now do the actual upload.
pushd /home/dean/software/appengine/python-latest
python2.7 ./appcfg.py --oauth2 update $WEBSITE_DIR
popd

