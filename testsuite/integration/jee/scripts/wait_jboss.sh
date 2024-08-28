#! /bin/bash

echo "## DOCKER: wait until jboss started... #####"
COUNTER=0
ATTEMPTS=60
JBOSS_SERVICE=$(docker-compose config --services | grep jboss)
while [ $COUNTER -lt $ATTEMPTS ]; do
	check=$( curl --digest --user root:shroot --retry 240 --retry-delay 2 --silent $(docker-compose port $JBOSS_SERVICE 9990)/management )
	if [[ ${check} ]]; then
		sleep 45
		echo "JBoss service is ready, moving on..."
		break
	fi
	echo "Waiting for JBoss service to start..."
	sleep 2
	COUNTER=$((COUNTER+1))
done
if [[ $COUNTER -eq ${ATTEMPTS} ]]; then
        echo "Error: Issue starting the JBoss service in the container, Exiting....."
        exit 1
fi