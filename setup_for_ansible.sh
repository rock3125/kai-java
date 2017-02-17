#!/bin/bash

# setup for ansible
gradle clean build createJarDependencies -x test

# setup web
cd web/admin
rm -rf dist/
grunt build

cd ../search
rm -rf dist/
grunt build

cd ../..
echo Ready: ansible-playbook ansible/setup-1.yml --extra-vars \"web_port=8085 server_port=10080 rf=1 external_name=peter-vm\"

