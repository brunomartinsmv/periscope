#!/usr/bin/env bash
#
# Installs legacy Maven artifacts that are no longer available from any live
# public Maven repository (PrimeFaces 3.4.2 community jar + bootstrap theme,
# and the Google-Code-hosted com.bigfatgun:fixjures 2.0-SNAPSHOT tree).
#
# The artifacts are vendored under tools/legacy-m2 (maven layout) so that this
# step is fully offline/reproducible and does not depend on the Wayback Machine
# or Google Code archive being reachable at run time.
#
# Idempotent: re-running simply reinstalls the files into the local repo.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENDOR="${SCRIPT_DIR}/legacy-m2"

# Prefer JDK 8 for Maven (the project targets Java 1.6 / Lucene 6 needs Java 8).
if [ -d /usr/lib/jvm/java-8-openjdk-amd64 ]; then
  export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
fi

install_file() {
  local file="$1" pom="$2"
  if [ -n "$file" ]; then
    mvn -q org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
      -Dfile="$file" -DpomFile="$pom"
  else
    mvn -q org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
      -Dfile="$pom" -DpomFile="$pom"
  fi
}

B="${VENDOR}/com/bigfatgun"
install_file "" "${B}/fixjures-project/2.0-SNAPSHOT/fixjures-project-2.0-SNAPSHOT.pom"
for a in fixjures-core fixjures-json fixjures-yaml fixjures; do
  d="${B}/${a}/2.0-SNAPSHOT"
  install_file "${d}/${a}-2.0-SNAPSHOT.jar" "${d}/${a}-2.0-SNAPSHOT.pom"
done

P="${VENDOR}/org/primefaces"
install_file "" "${P}/themes/themes-project/1.0.8/themes-project-1.0.8.pom"
install_file "${P}/primefaces/3.4.2/primefaces-3.4.2.jar" "${P}/primefaces/3.4.2/primefaces-3.4.2.pom"
install_file "${P}/themes/bootstrap/1.0.8/bootstrap-1.0.8.jar" "${P}/themes/bootstrap/1.0.8/bootstrap-1.0.8.pom"

echo "Legacy artifacts installed into local Maven repository."
