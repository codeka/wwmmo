#!/bin/bash

set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
INSTALLPATH=$ROOTPATH/website

# update the RESOURCE_VERSION in the file handlers/__init__.py so that
# all our resource references go to a different file. This is our cache-busting
# mechanism.
pushd $INSTALLPATH > /dev/null

sed -r 's/(RESOURCE_VERSION\s*=\s*)([0-9]+)/echo \1$((\2+1))/ge' handlers/__init__.py > /tmp/wwmmo.txt
mv /tmp/wwmmo.txt handlers/__init__.py

# now do the actual upload. we expect appcfg.py to be on the path
appcfg.py --oauth2 update .

popd > /dev/null

