#!/usr/bin/env bash
# Enables WildFly MicroProfile OpenAPI (smallrye) in standalone.xml via jboss-cli
# embed-server. Idempotent (try/catch: add only if the resource is missing).
#
# Usage:
#   JBOSS_HOME=/opt/jboss/wildfly-34.0.1.Final ./tools/enable-wildfly-openapi.sh
#
# Requires: JAVA_HOME pointing at a JDK that can run WildFly 34 (JDK 21).
# Stop the server before running, or run against a copy of the installation;
# embed-server edits standalone.xml on disk.
set -euo pipefail

JBOSS_HOME="${JBOSS_HOME:-/opt/jboss/wildfly-34.0.1.Final}"
CLI="${JBOSS_HOME}/bin/jboss-cli.sh"
CONF="${JBOSS_HOME}/standalone/configuration/standalone.xml"

if [[ ! -x "$CLI" ]]; then
  echo "jboss-cli.sh not found/executable: $CLI" >&2
  exit 1
fi
if [[ ! -f "$CONF" ]]; then
  echo "standalone.xml not found: $CONF" >&2
  exit 1
fi

# Prefer JDK 21 when present (WildFly 34); fall back to whatever JAVA_HOME is set.
if [[ -z "${JAVA_HOME:-}" && -d /usr/lib/jvm/java-21-openjdk-amd64 ]]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
fi

# try/catch: if read-resource fails (missing), add; if it succeeds, skip add.
# Real failures inside :add still fail the script (set -e + CLI non-zero).
CMDS='embed-server --server-config=standalone.xml'
CMDS+=',try,/extension=org.wildfly.extension.microprofile.openapi-smallrye:read-resource'
CMDS+=',catch,/extension=org.wildfly.extension.microprofile.openapi-smallrye:add'
CMDS+=',finally,end-try'
CMDS+=',try,/subsystem=microprofile-openapi-smallrye:read-resource'
CMDS+=',catch,/subsystem=microprofile-openapi-smallrye:add'
CMDS+=',finally,end-try'
CMDS+=',stop-embedded-server'

echo "Enabling MicroProfile OpenAPI in ${CONF} ..."
"$CLI" --commands="$CMDS"

if grep -q 'microprofile.openapi-smallrye' "$CONF" \
  && grep -q 'microprofile-openapi-smallrye' "$CONF"; then
  echo "OpenAPI enabled (extension + subsystem present)."
else
  echo "ERROR: OpenAPI extension/subsystem not found in $CONF after CLI" >&2
  exit 1
fi
