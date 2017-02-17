
if [ -e /etc/init.d/viki-webserver ]; then
    service viki-webserver stop
    /sbin/chkconfig --del viki-webserver
fi

cp /opt/kai/webserver/conf/webserver.properties /opt/kai/webserver/conf/webserver.properties.RPMSAVE
cp /opt/kai/webserver/conf/log4j2.xml /opt/kai/webserver/conf/log4j2.xml.RPMSAVE

exit 0
