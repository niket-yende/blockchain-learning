#!/bin/bash
echo "Connecting to docker"
echo $USER
sudo -H -u ubuntu bash -c 'docker-compose -f docker-compose-blockchainlayer.yaml up'
