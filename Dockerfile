# Multi-stage build: Maven (JDK 21) → WildFly 34 (JDK 21)
# Layer cache: POMs + vendored legacy repo first, then sources.

# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Parent + module POMs (and the file:// legacy-m2 repo referenced by the parent POM)
COPY pom.xml ./
COPY periscope-ejb/pom.xml periscope-ejb/
COPY periscope-web/pom.xml periscope-web/
COPY tools/legacy-m2 tools/legacy-m2

# Resolve dependencies before copying sources so source edits do not bust the cache
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY periscope-ejb periscope-ejb
COPY periscope-web periscope-web

RUN mvn -B clean package -DskipTests

# ---- Stage 2: runtime ----
FROM quay.io/wildfly/wildfly:34.0.1.Final-jdk21

ENV MONGODB_URI=mongodb://mongodb:27017 \
    MONGODB_DATABASE=Periscope \
    PERISCOPE_DIR=/opt/periscope

USER root
RUN mkdir -p /opt/periscope \
    && chown -R jboss:jboss /opt/periscope
# Enable MicroProfile OpenAPI via jboss-cli embed-server (no python3).
# Idempotent try/catch: add extension/subsystem only when missing.
# Real :add failures still fail the build (CLI non-zero).
USER jboss
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --commands='\
embed-server --server-config=standalone.xml,\
try,/extension=org.wildfly.extension.microprofile.openapi-smallrye:read-resource,\
catch,/extension=org.wildfly.extension.microprofile.openapi-smallrye:add,\
finally,end-try,\
try,/subsystem=microprofile-openapi-smallrye:read-resource,\
catch,/subsystem=microprofile-openapi-smallrye:add,\
finally,end-try,\
stop-embedded-server'

USER root
COPY --from=build --chown=jboss:jboss \
    /app/periscope-web/target/periscope.war \
    /opt/jboss/wildfly/standalone/deployments/periscope.war
USER jboss

EXPOSE 8080

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
