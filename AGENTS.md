# AGENTS.md

## Cursor Cloud specific instructions

Periscope is a Jakarta EE 10 patent-analysis web app (UFMT). It is a Maven
multi-module project: `periscope-ejb` (EJB jar, bundled into the war) and
`periscope-web` (war). There is **no EAR** — packaging is a single WAR
(`periscope-web/target/periscope.war`). Persistence is **MongoDB** via Morphia 2
(no JPA/SQL); harmonization uses **Lucene 9**; the UI is JSF / PrimeFaces 14
(Portuguese).

### Toolchain / runtime (already installed in the VM snapshot)
- **Build & run on JDK 21** (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`).
  Export this **before** every `mvn` invocation: `~/.bashrc` still points at
  Java 8, which does **not** work with the current modules
  (`maven.compiler.release=21`).
- **Maven 3** (`mvn`). `~/.m2/settings.xml` may still contain an override that
  stops Maven 3.8+ from hard-blocking plain-`http` repos; keep it if present.

### App server: WildFly 34.0.1.Final
- Install path: `/opt/jboss/wildfly-34.0.1.Final` (JDK 21).
- `~/.bashrc` may still export `JBOSS_HOME=/opt/jboss/wildfly-8.2.1.Final` — **always**
  `unset JBOSS_HOME` or `export JBOSS_HOME=/opt/jboss/wildfly-34.0.1.Final` before starting,
  otherwise `standalone.sh` boots the old server even from the WildFly 34 directory.
- Start:
  `unset JBOSS_HOME; export JBOSS_HOME=/opt/jboss/wildfly-34.0.1.Final JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64; cd "$JBOSS_HOME" && ./bin/standalone.sh -b 0.0.0.0`
- Deploy: copy `periscope-web/target/periscope.war` to
  `/opt/jboss/wildfly-34.0.1.Final/standalone/deployments/`
  (marker `periscope.war.deployed` on success; `.failed` on error).
- Context root `/periscope` via `WEB-INF/jboss-web.xml`.
- Copies of WildFly 8.2.1.Final and JBoss AS 7.1.1.Final may still exist under
  `/opt/jboss/` for reference only — **do not use them**.

### MongoDB (driver sync 5.x / Morphia 2)
- **MongoDB:** Morphia 2.5.3 + `mongodb-driver-sync` 5.2.1. Connection via
  `MONGODB_URI` (default `mongodb://localhost:27017`) and `MONGODB_DATABASE`
  (default `Periscope`). No auth in the local snapshot.
  Morphia **2.5.x** is required with driver 5.x (`MapCodec` is final in bson 5;
  Morphia 2.4.x cannot link). Jackson in the WAR is pinned to **2.17.0** (same
  line as WildFly 34) and server Jackson modules are excluded in
  `WEB-INF/jboss-deployment-structure.xml` so the seed YAML loader does not mix
  classloaders.
- Server: **MongoDB 4.4+** works for local smoke; Docker Compose targets
  **mongo:7**. Start local mongod (systemd is unavailable, run manually):
  `mongod --dbpath /var/lib/mongodb --bind_ip 127.0.0.1 --port 27017 --logpath /var/log/mongodb/mongod.log --fork`
  Do not restart MongoDB if it is already running in the VM.

### Docker
- **Docker is not available in this VM** — do not try to install it. Deliver
  `Dockerfile` / `docker-compose.yml` / `scripts/dev-up.sh` / `scripts/dev-down.sh`
  by code review; validate deploy manually on the local WildFly 34 install.

### Legacy Maven artifacts (vendored)
Still needed for the PrimeFaces theme `org.primefaces.themes:bootstrap:1.0.8`
(PrimeFaces itself comes from Maven Central as 14.x with classifier `jakarta`).
Vendored under `tools/legacy-m2/` (also contains obsolete `primefaces:3.4.2` and
`ionsjures` trees that are no longer referenced by the POMs).
`tools/install-legacy-artifacts.sh` installs them into `~/.m2` (idempotent);
run it if you wipe `~/.m2`. The parent POM also exposes
`file://${maven.multiModuleProjectDirectory}/tools/legacy-m2` as a repository.

### Build, deploy, run
- Build the WAR: `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 && mvn clean package`
  → `periscope-web/target/periscope.war` (no EAR).
- The app writes its Lucene index to **`/opt/periscope`**, which must exist and be
  writable by the server process (already created). Override with env `PERISCOPE_DIR`.
- App URL: `http://localhost:8080/periscope/`. Default login: **admin / 123456**
  (seeded into MongoDB on first deploy by `SeedBean`).

### Tests / lint
- There is no lint config and no runnable automated test suite. The only test file
  (`periscope-web/.../login.feature`) has no runner. `mvn test` compiles all modules
  and runs zero tests (surefire reports nothing). Treat `mvn clean package` as the
  compile/"lint" gate.
