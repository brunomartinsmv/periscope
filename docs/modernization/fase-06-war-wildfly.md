# Fase 6 — WAR Único, WildFly e Docker

**Branch de implementação:** `bruno/fase-6-war-wildfly-a4b3`  
**Depende de:** Fases 1–5  
**Bloqueia:** Fase 7  
**Estado:** concluída (validação manual em WildFly 34.0.1.Final local; Docker sem runtime nesta VM)

## Objetivo

Simplificar empacotamento para **WAR único**, configurar deploy em **WildFly 34.0.1.Final** com **Java 21**, e fornecer **Docker Compose** para ambiente local (substituto de homologação até existir ambiente dedicado).

---

## Decisões aplicadas

- Eliminar módulo `periscope-ear` (context-root `/periscope` migrado para `jboss-web.xml`)
- EJB embarcado no WAR (`WEB-INF/lib/periscope-ejb-*.jar`)
- WildFly **34.0.1.Final** (JDK 21) como servidor de aplicação
- Configuração via variáveis de ambiente (`MONGODB_URI`, `MONGODB_DATABASE`, `PERISCOPE_DIR`)
- Morphia **2.5.3** (compatível com mongodb-driver-sync **5.2.1**; 2.4.x não linka com bson 5)
- Jackson **2.17.0** no WAR (alinhado ao WildFly 34) + exclusão dos módulos Jackson do servidor

---

## Tarefas

### 6.1 Eliminar módulo EAR — feito

Removido `periscope-ear/` e, no POM raiz, o módulo, o `dependencyManagement` de `periscope-web` (só servia ao EAR) e o `maven-ear-plugin`.

### 6.2 Configurar WAR único — feito

`periscope-web/pom.xml`: `<finalName>periscope</finalName>` + dependência `periscope-ejb` (type `ejb`, scope compile).

**Resultado:** `periscope-web/target/periscope.war`

### 6.3 Configuração WildFly — feito

- `WEB-INF/jboss-web.xml`: `<context-root>/periscope</context-root>`
- `WEB-INF/jboss-deployment-structure.xml`: exclusões **testadas** dos módulos
  `com.fasterxml.jackson.*` / `resteasy-jackson2-provider` do WildFly 34 (sem isso o
  SeedBean falhava com `NoSuchMethodError` em `YAMLParser._updateToken` por mistura
  2.17 servidor × 2.18 WAR). Lucene/Morphia/Mongo **não** exigiram exclusão.

### 6.4 Variáveis de ambiente

| Variável | Default (código) | Descrição |
|----------|------------------|-----------|
| `MONGODB_URI` | `mongodb://localhost:27017` | URI MongoDB |
| `MONGODB_DATABASE` | `Periscope` | Nome do banco |
| `PERISCOPE_DIR` | `/opt/periscope` | Índice Lucene |

No Compose: `MONGODB_URI=mongodb://mongodb:27017`.

### 6.5–6.7 Docker / scripts / README — feitos

- `Dockerfile` multi-stage: `maven:3.9-eclipse-temurin-21` → `quay.io/wildfly/wildfly:34.0.1.Final-jdk21`, WAR em `/opt/jboss/wildfly/standalone/deployments/`, ENV Mongo/Lucene, `/opt/periscope` com owner `jboss`, `EXPOSE 8080`, cache de camadas (POMs + `tools/legacy-m2` antes das fontes).
- `docker-compose.yml`, `.dockerignore`, `scripts/dev-up.sh` / `dev-down.sh`
- README e AGENTS.md atualizados

**Docker Compose não executado nesta VM** (Docker ausente).

### Ajustes de runtime descobertos no deploy WildFly 34

Necessários para o WAR subir (além do empacotamento):

- CDI: beans `@ViewScoped` / dependências passivation-capable (`Serializable` ou `@ApplicationScoped` / `@RequestScoped`)
- Morphia 2.5: remover `@Entity`+`@Embedded` em `Country`/`ApplicantType`; adicionar `@Id ObjectId`
- Analyzers Lucene: **não** `@ApplicationScoped` (métodos `final` do `Analyzer` → não proxyáveis)

---

## Critérios de aceite

- [x] `mvn clean package` gera `periscope-web/target/periscope.war` (sem EAR)
- [ ] `docker compose up --build` — **não validado** (sem Docker na VM; Dockerfile/compose revisados estaticamente)
- [x] App em `http://localhost:8080/periscope/` (WildFly 34 local; marcador `periscope.war.deployed`)
- [x] Login JSF (`admin` / `123456`) → redirect para `projectList.jsf`; usuário seeded no MongoDB
- [ ] Importação e harmonização end-to-end — fora do smoke mínimo desta fase
- [x] Variáveis de ambiente documentadas e honradas (`Resources` / `SeedBean`)
- [x] README e AGENTS.md atualizados

## Riscos

| Risco | Mitigação |
|-------|-----------|
| Jackson WildFly × WAR | Exclusões em `jboss-deployment-structure.xml` + Jackson 2.17.0 |
| Morphia × driver 5 | Morphia 2.5.3 |
| `/opt/periscope` | Volume Compose + `chown jboss`; no host local o dir já é gravável |

## Homologação futura

Reutilizar `Dockerfile` / `docker-compose.yml`, acrescentando reverse proxy, secrets e tags de release.
