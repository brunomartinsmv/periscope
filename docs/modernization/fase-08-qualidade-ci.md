# Fase 8 — Qualidade, CI/CD e Homologação Futura

**Branches:** `bruno/fase-8a-ci-testes-*` (8a) · `bruno/fase-8b-openapi-e2e-*` (8b)  
**Depende de:** Fases 0–7 (WAR único, WildFly 34, Java 21, API REST, SPA)

## Objetivo

Estabelecer **pipeline de qualidade**, **testes automatizados**, **OpenAPI** e **preparação para ambiente de homologação** que ainda não existe hoje.

---

## Fase 8a — CI + unitários + IT + health

### 8.1 CI — GitHub Actions

**Arquivo:** `.github/workflows/ci.yml`

| Job | Comando | Notas |
|-----|---------|-------|
| `build` | `mvn -B clean verify` | Unitários (sem Docker/Mongo) |
| `integration` | `mvn -B verify -Pit` | Testcontainers (`mongo:7`); Docker no Actions |
| `frontend` | `npm ci && npm run lint && npm run build` | Node 22 em `periscope-ui/` (Fase 8b) |
| `dependency-check` | `mvn org.owasp:dependency-check-maven:check` | `continue-on-error: true` + artifact |
| `e2e` | Playwright | **Só** `workflow_dispatch` (precisa WildFly+Mongo) |

Triggers: `push` / `pull_request` em `master` + `workflow_dispatch`.

### 8.2 Testes unitários (JUnit 5 + Mockito + AssertJ)

| Classe | O que cobre |
|--------|-------------|
| `FastJoinRegressionTest` | Lucene 9 Fast-Join + 2 casos novos de similaridade |
| `YamlLoaderTest` | Seeds YAML (países, admin, tipos, descritores) |
| `PDFTextParserTest` | Extração PDFBox 3 (PDF gerado no teste) |
| `ESPACENETPatentImporterTest` | XLS mínimo via POI HSSF |
| `PATENTSCOPEPatentImporterTest` | XLS mínimo via POI HSSF |
| `DPMAPatentImporterTest` | Export texto `; ` (+ fixture `importer/dpma-sample.csv`) |

Local: `periscope-ejb/src/test/java/`.

### 8.3 Testes de integração — Testcontainers

- Dependência: `org.testcontainers:mongodb:1.20.4` (+ junit-jupiter)
- Perfil Maven **`it`**: `maven-failsafe-plugin` inclui `*IT.java`
- Classe: `PatentRepositoryIT` — `MongoDBContainer("mongo:7")`, Morphia save/find de `Patent`/`Project`
- `@Testcontainers(disabledWithoutDocker = true)` + `assumeTrue` — sem Docker os ITs são pulados

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B clean verify          # unitários only
mvn -B clean verify -Pit     # + ITs (skip se sem Docker)
```

### 8.6 Análise de dependências (CVE)

`dependency-check-maven` **11.x** em `pluginManagement` (execução manual/CI, **fora** do ciclo padrão).

### 8.7 Cobertura

JaCoCo **0.8.12** (`prepare-agent` + `report`) no POM raiz.

### 8.9 Health endpoint

- `GET /periscope/rest/health` → JSON `status` / `mongodb` / `luceneIndex`
- HTTP 200 se tudo UP; 503 se DOWN

---

## Fase 8b — OpenAPI + E2E Playwright + CI frontend (**concluída**)

### OpenAPI 3 (MicroProfile / WildFly smallrye)

- Anotações `@OpenAPIDefinition` / `@SecurityScheme` (Bearer JWT) em `JaxRsActivator`
- `@Tag` por resource e `@Operation` nos endpoints principais
- Dependência `microprofile-openapi-api` (provided)
- Config: `META-INF/microprofile-config.properties` (`mp.openapi.extensions.enabled=true`)
- Subsystem WildFly: `tools/enable-wildfly-openapi.sh` (também aplicado no `Dockerfile`)
- Documento: **`GET http://localhost:8080/openapi`** (YAML/JSON; endpoint nativo do subsystem WildFly na raiz do host)

```bash
# Habilitar subsystem (uma vez por instalação WildFly; parar o servidor antes) e reiniciar
JBOSS_HOME=/opt/jboss/wildfly-34.0.1.Final ./tools/enable-wildfly-openapi.sh

curl -s http://localhost:8080/openapi | head
# Accept: application/json também funciona
curl -s -H 'Accept: application/json' http://localhost:8080/openapi | python3 -c \
  'import sys,json; d=json.load(sys.stdin); print(d["info"]["title"], len(d["paths"]))'
```

> A exclusão de módulos Jackson em `jboss-deployment-structure.xml` **não** impede o OpenAPI:
> o documento é servido pelo subsystem `microprofile-openapi-smallrye` (módulos do servidor),
> e o WAR declara dependência explícita de `org.eclipse.microprofile.openapi.api`.
> Não há servlet de proxy no WAR — a URL canônica é `/openapi`.

### E2E Playwright (`periscope-ui`)

```bash
cd periscope-ui
npm ci
npx playwright install --with-deps chromium   # ou sem --with-deps se apt/sudo falhar
# Backend + SPA no WAR em /periscope/app/ (ou E2E_BASE_URL=http://localhost:5173/periscope/app/)
npm run test:e2e
# UI interativa:
npm run test:e2e:ui
```

Specs em `periscope-ui/e2e/`: `login`, `projects`, `patents`, `reports`, `harmonization`.

### CI frontend + template de deploy

- Job `frontend` (Node 22): `npm ci` / `lint` / `build`
- Job `e2e`: `if: github.event_name == 'workflow_dispatch'` — passos documentados (Mongo service, WAR, WildFly, Playwright)
- Template inerte: `.github/workflows/deploy-staging.yml` (`workflow_dispatch` + `if: false`)

### Comandos de validação (8b)

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B clean verify
curl -sf http://localhost:8080/periscope/rest/health
curl -sf http://localhost:8080/openapi | head
curl -sf -o /dev/null -w "%{http_code}\n" http://localhost:8080/periscope/app/
curl -sf -o /dev/null -w "%{http_code}\n" http://localhost:8080/periscope/login.jsf
cd periscope-ui && npm run lint && npm run build && npm run test:e2e
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml')); yaml.safe_load(open('.github/workflows/deploy-staging.yml')); print('ok')"
```

---

## Critérios de aceite

### 8a
- [x] `mvn -B clean verify` SUCCESS com testes unitários
- [x] `mvn -B verify -Pit` SUCCESS (ITs reais no CI; skip gracioso sem Docker)
- [x] Workflow CI válido (YAML)
- [x] Health `200` + `"status":"UP"` no WildFly 34
- [x] Login JSF sem regressão

### 8b
- [x] OpenAPI em `/openapi` com ≥10 paths reais
- [x] Playwright E2E verde contra backend real (`npm run test:e2e`)
- [x] Job CI `frontend` (lint + build)
- [x] Job CI `e2e` desabilitado por padrão (`workflow_dispatch`)
- [x] Template `deploy-staging.yml` inerte
- [x] Docs / README / AGENTS atualizados
