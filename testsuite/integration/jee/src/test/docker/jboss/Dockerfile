FROM armdocker.rnd.ericsson.se/proj_oss/autoprovisioning/jboss_service:latest

COPY standalone-enm.xml $JBOSS_HOME/standalone/configuration/standalone-enm.xml

ADD jboss-as.conf /ericsson/3pp/jboss/**

ADD neo4j.properties /ericsson/3pp/jboss/standalone/data/dps/credentials/neo4j.properties

USER root
RUN mkdir -p /ericsson/3pp/jboss/domain/configuration
RUN touch /ericsson/3pp/jboss/domain/configuration/mgmt-groups.properties
RUN touch /ericsson/3pp/jboss/domain/configuration/mgmt-users.properties
RUN chown jboss_user:jboss /ericsson/3pp/jboss/domain/configuration/mgmt-users.properties
RUN chown jboss_user:jboss /ericsson/3pp/jboss/domain/configuration/mgmt-groups.properties
RUN /ericsson/3pp/jboss/bin/add-user.sh -u root -p shroot