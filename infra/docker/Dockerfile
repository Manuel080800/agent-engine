FROM node:22-bookworm-slim AS frontend
WORKDIR /workspace/webapp/src/main/frontend
COPY webapp/src/main/frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY webapp/src/main/frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY core/ core/
COPY api/ api/
COPY webapp/pom.xml webapp/pom.xml
COPY webapp/src/main/java/ webapp/src/main/java/
COPY webapp/src/main/resources/ webapp/src/main/resources/
COPY webapp/src/main/webapp/ webapp/src/main/webapp/
COPY --from=frontend /workspace/webapp/target/frontend/ webapp/target/frontend/
# Frontend is already compiled in the Node stage. Skip its Maven execs here.
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode -Dexec.skip=true -DskipTests package

FROM quay.io/wildfly/wildfly:41.0.1.Final-jdk21
USER root
RUN microdnf install -y util-linux && microdnf clean all
USER jboss
COPY --chown=jboss:root infra/wildfly/configure.cli /opt/jboss/configure.cli
RUN TLS_PASSWORD=build-only-placeholder /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/configure.cli && rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history
COPY --from=build --chown=jboss:root /workspace/api/target/api.war /opt/jboss/wildfly/standalone/deployments/api.war
COPY --from=build --chown=jboss:root /workspace/webapp/target/ROOT.war /opt/jboss/wildfly/standalone/deployments/ROOT.war
COPY --chown=jboss:root infra/docker/entrypoint.sh /opt/jboss/entrypoint.sh
USER root
EXPOSE 8443
ENTRYPOINT ["/bin/bash","/opt/jboss/entrypoint.sh"]
