FROM jboss/wildfly:18.0.1.Final
MAINTAINER Marco Spasiano <marco.spasiano@cnr.it>

USER root
RUN mkdir -p /var/log/govpay;chown -R jboss:jboss /var/log/govpay
USER jboss

COPY ./docker/standalone.xml /opt/jboss/wildfly/standalone/configuration/
COPY ./docker/postgresql/postgresql-9.2-1004.jdbc41.jar /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/postgresql-9.2-1004.jdbc41.jar
COPY ./docker/postgresql/module.xml /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/module.xml
COPY ./ear/target/govpay.ear /opt/jboss/wildfly/standalone/deployments/govpay.ear

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "--server-config=standalone.xml"]
