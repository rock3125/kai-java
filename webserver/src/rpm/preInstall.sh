if [ -e /etc/init.d/viki-webserver ]; then
    service viki-webserver stop
fi

if [ ! -d /opt/kai/webserver ]; then
    /bin/mkdir -p /opt/kai/webserver/bin
    /bin/mkdir -p /opt/kai/webserver/conf
    /bin/mkdir -p /opt/kai/webserver/web
    /bin/chown -R peter. /opt/kai/webserver
fi

exit 0
