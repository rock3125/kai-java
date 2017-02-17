#!/bin/bash

# build sites
cd web/admin
grunt build
if [ $? -ne 0 ]; then
echo "grunt build admin failed"
exit 1
fi

cd ../search
grunt build
if [ $? -ne 0 ]; then
echo "grunt build search failed"
exit 1
fi

cd ../..

# build java dist
gradle clean build createJarDependencies -x test

echo to complete the deployment:
echo ansible-playbook ansible/setup-1.yml --extra-vars \"web_port=8085 server_port=10080 rf=1 external_name=vm-name\"
