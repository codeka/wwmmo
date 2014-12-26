#!/bin/bash

set -e

SCRIPT=`realpath $0`
SCRIPTPATH=`dirname $SCRIPT`
ROOTPATH=`dirname $SCRIPTPATH`
WEBSITEPATH=$ROOTPATH/website

dev_appserver.py --host=0.0.0.0 --port=8272 \
	--datastore_path=/tmp/wwmmo-datastore.dat \
        --automatic_restart --dev_appserver_log_level=debug \
        --enable_sendmail=true \
	$WEBSITEPATH

