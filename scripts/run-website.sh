#!/bin/bash

pushd /home/dean/software/appengine/python-latest
python2.7 ./dev_appserver.py --host=0.0.0.0 --port=8272 \
	--datastore_path=/home/dean/data/wwblog/datastore.dat \
        --automatic_restart --dev_appserver_log_level=debug \
        --enable_sendmail=true \
	/home/dean/software/wwmmo/code/website
popd

