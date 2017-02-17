#!/bin/sh -

LOG=/var/log/kai
export JAVA=/opt/jdk

cd /opt/kai/node01/bin

CP=`echo *.jar | tr ' ' ':'`
$JAVA/bin/java -cp $CP \
-Dlog4j.configurationFile=/opt/kai/node01/conf/log4j2.xml \
-Xmx3G \
industries.vocht.viki.main.SpringApplication 2>$LOG/node01-stdout.log &

exit 0
