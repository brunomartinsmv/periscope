# AGENTS.md

## Cursor Cloud specific instructions

Periscope is a Java EE 6 patent-analysis web app (UFMT). It is a Maven multi-module
project: `periscope-ejb` (EJB jar, also bundled into the war), `periscope-web`
(war), `periscope-ear` (ear). Persistence is **MongoDB** via Morphia (no JPA/SQL);
harmonization uses **Lucene 6**; the UI is JSF 2 + PrimeFaces 3.4.2 (Portuguese).

### Toolchain / runtime (already installed in the VM snapshot)
- **Build & run on JDK 8** (`JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64`). This is
  exported in `~/.bashrc`. Java 21 is the machine default but does NOT work: the
  modules target source/target 1.6 (unsupported by JDK 21) and the runtime stack
  below requires Java 8.
- **Maven 3** (`mvn`), uses `~/.m2/settings.xml` which contains an override that
  stops Maven 3.8+ from hard-blocking the (now-dead) plain-`http` repos declared in
  the poms. Do not delete it.

### App server: WildFly 8.2.1.Final (NOT JBoss AS 7.1.1)
- The README targets JBoss AS 7.1.1.Final, but AS 7.1.1 **cannot run on JDK 8**
  (its `DeployerChainAddHandler` violates the comparator contract and JDK 8's
  `ConcurrentSkipListMap` infinite-loops during boot — no config fix exists). Since
  the app needs a Java 8 runtime (Lucene 6.0.0 is Java-8 bytecode), we run it on
  **WildFly 8.2.1.Final** (the direct successor to JBoss AS 7), installed at
  `/opt/jboss/wildfly-8.2.1.Final`. It hosts the EE6 app unchanged.
- A copy of JBoss AS 7.1.1.Final is at `/opt/jboss/jboss-as-7.1.1.Final` for
  reference only; do not use it.

### MongoDB 4.4 (required version)
- The app bundles `mongo-java-driver-2.11.0`, which speaks the legacy wire protocol
  removed in MongoDB 5.1+. Use **MongoDB 4.4** (installed). It listens on
  `localhost:27017`, database `Periscope`, **no auth**. `new Mongo()` with defaults.
- Start it (systemd is unavailable, run manually):
  `mongod --dbpath /var/lib/mongodb --bind_ip 127.0.0.1 --port 27017 --logpath /var/log/mongodb/mongod.log --fork`

### Legacy Maven artifacts (vendored)
Three dependency trees are gone from every live public repo and are vendored under
`tools/legacy-m2/` (maven layout): `org.primefaces:primefaces:3.4.2`,
`org.primefaces.themes:bootstrap:1.0.8` (recovered from the Wayback Machine) and
`com.bigfatgun:fixjures:2.0-SNAPSHOT` + its `fixjures-core/json/yaml` modules
(recovered from the Google Code source archive). `tools/install-legacy-artifacts.sh`
installs them into `~/.m2`. This runs in the startup update script and is idempotent;
run it manually if you ever wipe `~/.m2`.

### Build, deploy, run
- Build the EAR: `mvn clean package` → `periscope-ear/target/periscope.ear`.
- The app writes its Lucene index to **`/opt/periscope`**, which must exist and be
  writable by the server process (already created). Override with env `PERISCOPE_DIR`.
- Start WildFly: `cd /opt/jboss/wildfly-8.2.1.Final && ./bin/standalone.sh -b 0.0.0.0`.
- Deploy: `cp periscope-ear/target/periscope.ear /opt/jboss/wildfly-8.2.1.Final/standalone/deployments/`
  (a `periscope.ear.deployed` marker appears on success; `.failed` on error).
- App URL: `http://localhost:8080/periscope/`. Default login: **admin / 123456**
  (seeded into MongoDB on first deploy by `SeedBean`).

### Tests / lint
- There is no lint config and no runnable automated test suite. The only test file
  (`periscope-web/.../login.feature`) has no runner. `mvn test` compiles all modules
  and runs zero tests (surefire reports nothing). The README's Arquillian test
  requires a remote container profile (`-Parq-jbossas-remote`) and is skipped by
  default; no such test sources are present. Treat `mvn clean package` as the
  compile/"lint" gate.
