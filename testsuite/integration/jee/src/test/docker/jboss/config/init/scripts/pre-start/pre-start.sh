#! /bin/bash

# This script is executed before JBoss, so it can be used to configure the environment.
#--------------------------------------------------------------------------------------

# Load the common functions
source docker-env-functions.sh

#startup.sh -NSJ
wait_postgres
wait_container "neo4j"
