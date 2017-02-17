
if [ -e /etc/init.d/node01 ]; then
    service node01 stop
fi

if [ ! -d /opt/kai/server ]; then
    /bin/mkdir -p /opt/kai/node01
    /bin/mkdir -p /opt/kai/node01/web
    /bin/chown -R viki. /opt/kai/node01
fi

exit 0
