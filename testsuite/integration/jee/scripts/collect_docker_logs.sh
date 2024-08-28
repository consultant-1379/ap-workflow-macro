#! /bin/bash

echo "#### DOCKER: copying docker logs to workspace #####"

# Script that writes the docker container logs to workspace
while read container; do
    docker logs ${container} >$WORKSPACE/${container}.log
done < <(docker ps --format '{{.Names}}')