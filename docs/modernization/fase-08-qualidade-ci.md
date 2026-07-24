# Fase 8 — Qualidade, CI/CD e Homologação Futura

**Branch / PR:** `bruno/fase-8a-ci-testes-*` (Fase **8a**)  
**Depende de:** Fases 0–6 (WAR único, WildFly 34, Java 21)  
**Paralelo com:** Fase 7 (SPA)  
**Segue em:** Fase **8b** (OpenAPI, E2E)

## Objetivo

Estabelecer **pipeline de qualidade**, **testes automatizados** e **preparação para ambiente de homologação** que ainda não existe hoje, mas será necessário no futuro.

Esta entrega cobre a **Fase 8a** (CI + unitários + IT com Testcontainers + JaCoCo + dependency-check + health). Itens de OpenAPI e E2E ficam para a **8b**.

---

## Contexto

Sem homologação hoje, a validação depende de:
1. Testes automatizados locais / CI
2. Docker Compose como ambiente reproduzível (Fase 6)
3. Checklists manuais (Fase 0)
4. Health HTTP em `/periscope/rest/health`

---

## Fase 8a — feito nesta entrega

### 8.1 CI — GitHub Actions

**Arquivo:** `.github/workflows/ci.yml`

| Job | Comando | Notas |
|-----|---------|-------|
| `build` | `mvn -B clean verify` | Unitários (sem Docker/Mongo) |
| `integration` | `mvn -B verify -Pit` | Testcontainers (`mongo:7`); Docker no Actions |
| `dependency-check` | `mvn org.owasp:dependency-check-maven:check` | `continue-on-error: true` + artifact |

Triggers: `push` / `pull_request` em `master`. Sem job de frontend (SPA ainda não existe).

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
- `@Testcontainers(disabledWithoutDocker = true)` + `assumeTrue` — sem Docker os ITs são pulados (esta VM não tem Docker; CI tem)

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B clean verify          # unitários only
mvn -B clean verify -Pit     # + ITs (skip se sem Docker)
```

### 8.6 Análise de dependências (CVE)

`dependency-check-maven` **11.x** em `pluginManagement` (execução manual/CI, **fora** do ciclo padrão).

### 8.7 Cobertura

JaCoCo **0.8.12** (`prepare-agent` + `report`) no POM raiz.

### 8.8 Formatação

- `.editorconfig` na raiz (Java 4 espaços; POM 3 espaços)
- **Spotless não adicionado** — sem `ratchetFrom` confiável nesta linha do tempo sem gerar reformatação em massa do legado

### 8.9 Health endpoint

- `GET /periscope/rest/health` → JSON `status` / `mongodb` / `luceneIndex`
- HTTP 200 se tudo UP; 503 se DOWN
- Corpo JSON montado como `String` (Jackson JAX-RS do servidor permanece excluído)
- `UserAccessFilter` só cobre `/pages/*` e `*.jsf` — `/rest/*` livre

### Dataset de teste

Ver `docs/modernization/dataset-teste/` e `periscope-ejb/src/test/resources/importer/`.

---

## Fase 8b — pendente

| Item | Motivo de adiamento |
|------|---------------------|
| OpenAPI / swagger das APIs REST | Depende da expansão REST (Fase 7) |
| E2E (Cucumber `login.feature` / Playwright) | Sem runner hoje; SPA ainda não existe |
| Job CI de frontend | `periscope-ui` ainda não criado |
| Meta de cobertura 40%+ com gate | Baseline JaCoCo existe; gate em fase seguinte |
| Homologação operacional | Infra ainda inexistente — ver `ambiente-homologacao-futuro.md` |

---

## Critérios de aceite (8a)

- [x] `mvn -B clean verify` SUCCESS com testes unitários novos
- [x] `mvn -B verify -Pit` SUCCESS (ITs reais no CI; skip gracioso sem Docker)
- [x] Workflow CI válido (YAML)
- [x] Health `200` + `"status":"UP"` no WildFly 34
- [x] Login JSF sem regressão
- [x] Documentação atualizada (`AGENTS.md`, README modernização, dataset)
