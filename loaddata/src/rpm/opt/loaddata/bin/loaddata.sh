#!/bin/bash

export JAVA_HOME=/opt/jdk
cd /opt/kai/loaddata/bin

$JAVA_HOME/bin/java -cp "/opt/kai/loaddata/bin/*" \
-Dlog4j.configurationFile=/opt/kai/loaddata/conf/log4j2.xml \
industries.vocht.viki.loaddata.DataLoader "$@"
