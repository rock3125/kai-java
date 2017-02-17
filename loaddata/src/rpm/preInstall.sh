
if [ ! -d /opt/kai/loaddata ]; then
    /bin/mkdir -p /opt/kai/loaddata/bin
    /bin/mkdir -p /opt/kai/loaddata/conf
    /bin/chown -R peter. /opt/kai/loaddata
fi

exit 0
