#!/bin/bash

KEYFILE="/data/db/mongo-keyfile"

if [ ! -f $KEYFILE ]; then
  echo 'mySuperSecretMongoKeyfileForReplicaSet123456' > $KEYFILE
  chmod 400 $KEYFILE
  chown 999:999 $KEYFILE
fi

exec docker-entrypoint.sh mongod --replSet rs0 --bind_ip_all --port 27017 --keyFile $KEYFILE