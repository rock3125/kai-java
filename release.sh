#!/bin/bash

if [ $# -ne 1 ]
  then
    echo "takes one parameter: version"
    echo "the version to release / build"
    exit 0
fi

RELEASE=$1

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

# build system and run unit tests
gradle clean build

# proceed only if gradle succeeded
if [ $? -eq 0 ]; then

# tag
git tag "$RELEASE" master
git push --tags

# re-build the rpms (new version) - skip unit tests at this point
# gradle clean build rpm -x test

# build JAR dependencies skip unit tests at this point
gradle clean build createJarDependencies -x test

# to complete the deployment, ansible-playbook ansible/setup-cluster-one[-https].yml

fi
