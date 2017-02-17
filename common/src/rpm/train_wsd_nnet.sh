#!/bin/bash

# setup files on remote
# scp common/build/jars/* user@remote:/path
# scp common/libs/* user@remote:/path
# scp common/rpm/*.sh user@remote:/path

java -cp "*" \
industries.vocht.viki.nnet_wsd_create.CreateNNet "$@"
