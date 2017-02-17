
if [ -e /etc/init.d/node01 ]; then
    service node01 stop
    /sbin/chkconfig --del node01
fi

cp /opt/kai/node01/server.properties /opt/kai/node01/server.properties.RPMSAVE
cp /opt/kai/node01/log4j2.xml /opt/kai/node01/log4j2.xml.RPMSAVE

exit 0
