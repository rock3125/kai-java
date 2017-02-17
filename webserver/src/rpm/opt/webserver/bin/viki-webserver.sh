#!/bin/sh -

LOG=/var/log/kai
export JAVA=/opt/jdk

cd /opt/kai/webserver/bin

CP=`echo *.jar | tr ' ' ':'`
$JAVA/bin/java -cp $CP \
-Dlog4j.configurationFile=/opt/kai/webserver/conf/log4j2.xml \
industries.vocht.viki.webserver.Main 2>$LOG/webserver-stdout.log &
