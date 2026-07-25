# Periscope

Sistema web de **análise e harmonização de patentes** (UFMT). Oferece duas interfaces
sobre o mesmo backend Jakarta EE:

| Interface | URL | Status |
|-----------|-----|--------|
| **SPA React** (`periscope-ui`) | http://localhost:8080/periscope/app/ | Principal |
| **JSF / PrimeFaces 14** (legado) | http://localhost:8080/periscope/ | Fallback |

Persistência em **MongoDB** (Morphia); harmonização de nomes com **Lucene** (Fast-Join).
Login padrão: `admin` / `123456` (seed no primeiro deploy).

Ver também: [CHANGELOG.md](CHANGELOG.md) · [docs/modernization/](docs/modernization/README.md) ·
[periscope-ui/README.md](periscope-ui/README.md)

## Stack atual

| Componente | Versão / detalhe |
|------------|------------------|
| Java | 21 |
| Jakarta EE | 10 |
| Servidor | WildFly 34.0.1.Final |
| UI legado | JSF + PrimeFaces 14 (`classifier: jakarta`) |
| UI moderna | React 19 + Vite + TypeScript (`periscope-ui`) |
| Persistência | MongoDB + Morphia **2.5.3** + mongodb-driver-sync **5.2.1** |
| Harmonização | Lucene **9.12.0** |
| PDF / planilhas | PDFBox **3.0.3** · Apache POI **5.3.0** |
| Empacotamento | **WAR único** (`periscope-web` + EJB embarcado) |
| API | JAX-RS em `/periscope/rest` (JWT HS256) |
| Docs da API | OpenAPI 3 em `/openapi` |

### Módulos

```
periscope/                 # POM pai
├── periscope-ejb/         # EJB JAR (domínio, Morphia, Lucene, importadores)
├── periscope-web/         # WAR (JSF + API REST + SPA embarcada em webapp/app/)
└── periscope-ui/          # Frontend React (build separado via npm)
```

Artefato de deploy: `periscope-web/target/periscope.war`.

## Requisitos

**Opção A — Docker:** Docker e Docker Compose.

**Opção B — Manual:** JDK 21, Maven 3.9+, Node 22 (só para a SPA), MongoDB 4.4+
(7.x recomendado) e WildFly 34.0.1.Final (JDK 21).

## Quick start (Docker Compose)

```bash
./scripts/dev-up.sh
# ou: docker compose up --build -d
```

| Recurso | URL |
|---------|-----|
| SPA | http://localhost:8080/periscope/app/ |
| JSF (fallback) | http://localhost:8080/periscope/ |
| API REST | http://localhost:8080/periscope/rest/ |
| OpenAPI | http://localhost:8080/openapi |
| Health | http://localhost:8080/periscope/rest/health |

Login: `admin` / `123456`. Parar: `./scripts/dev-down.sh`.

Serviços: `mongodb` (`mongo:7`) e `periscope` (WildFly 34 + WAR). A imagem habilita o
subsystem MicroProfile OpenAPI via `jboss-cli`.

## Build e deploy manual (WildFly 34)

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # ajuste se necessário

# Tema bootstrap 1.0.8 (vendored), se ~/.m2 estiver limpo:
./tools/install-legacy-artifacts.sh

# (Opcional) embarcar a SPA no WAR
cd periscope-ui && npm ci && npm run build:war && cd ..

mvn clean package
# → periscope-web/target/periscope.war

# MongoDB local (exemplo)
# mongod --dbpath /var/lib/mongodb --bind_ip 127.0.0.1 --port 27017 --fork

# Índice Lucene
mkdir -p /opt/periscope

# WildFly (JBOSS_HOME deve apontar para o 34 — evite o 8.2 legado no PATH)
unset JBOSS_HOME
export JBOSS_HOME=/opt/jboss/wildfly-34.0.1.Final
cd "$JBOSS_HOME" && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./bin/standalone.sh -b 0.0.0.0

# Em outro terminal — OpenAPI (uma vez por instalação) e deploy
JBOSS_HOME=/opt/jboss/wildfly-34.0.1.Final ./tools/enable-wildfly-openapi.sh   # com o servidor parado
cp periscope-web/target/periscope.war \
   /opt/jboss/wildfly-34.0.1.Final/standalone/deployments/
```

Sucesso: marcador `periscope.war.deployed`. Sem a SPA embarcada, `/periscope/app/`
fica indisponível; JSF e a API REST continuam funcionando.

## Variáveis de ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `MONGODB_URI` | `mongodb://localhost:27017` | URI do MongoDB |
| `MONGODB_DATABASE` | `Periscope` | Nome do banco |
| `PERISCOPE_DIR` | `/opt/periscope` | Diretório do índice Lucene (deve ser gravável) |
| `PERISCOPE_JWT_SECRET` | *(efêmero em memória)* | Segredo HS256 da API REST; **defina em produção** |
| `PERISCOPE_JWT_EXPIRATION_HOURS` | `8` | Validade do JWT |
| `PERISCOPE_CORS_ORIGINS` | `http://localhost:5173` | Origins CORS (separadas por vírgula) |

No Docker Compose, `MONGODB_URI` aponta para o serviço `mongodb`.

## API REST

Base: `http://localhost:8080/periscope/rest`

| Área | Endpoints (resumo) |
|------|--------------------|
| Auth | `POST /auth/login`, `GET /auth/me`, `POST /auth/logout` |
| Projetos | CRUD `/projects` |
| Patentes | `/projects/{id}/patents`, `/patents/{id}`, import multipart |
| Harmonização | sugestões Fast-Join, regras, apply |
| Relatórios | `main-applicant`, `main-inventor`, `main-ipc`, `application-date`, `publication-date` |
| Usuários | CRUD `/users` (ADMIN) |
| Arquivos | GridFS upload/download |
| Health | `GET /health` → `{"status":"UP",...}` |

Autenticação: `Authorization: Bearer <token>`. Documentação viva:

```bash
curl -s http://localhost:8080/openapi
# JSON: curl -s -H 'Accept: application/json' http://localhost:8080/openapi
```

Exemplo:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/periscope/rest/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' | jq -r .token)
curl -s http://localhost:8080/periscope/rest/auth/me -H "Authorization: Bearer $TOKEN"
```

Detalhes: [docs/modernization/fase-07-spa.md](docs/modernization/fase-07-spa.md).

## SPA React (`periscope-ui`)

```bash
cd periscope-ui
npm ci
npm run dev
# → http://localhost:5173/periscope/app/  (proxy para /periscope/rest no WildFly)
```

Embarcar no WAR (Maven **não** executa Node; `webapp/app/` é gitignored):

```bash
cd periscope-ui && npm run build:war
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B package
cp periscope-web/target/periscope.war /opt/jboss/wildfly-34.0.1.Final/standalone/deployments/
# → http://localhost:8080/periscope/app/
```

## Testes

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Unitários Maven (~37 testes)
mvn -B clean verify

# Integração (Testcontainers MongoDB 7; skip sem Docker)
mvn -B verify -Pit

# Frontend
cd periscope-ui && npm ci && npm run lint && npm run build

# E2E Playwright (requer WildFly + Mongo + SPA em /periscope/app/)
npx playwright install --with-deps chromium
npm run test:e2e
# override: E2E_BASE_URL=http://localhost:5173/periscope/app/ npm run test:e2e
```

## CI

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

| Job | Quando | O que faz |
|-----|--------|-----------|
| `build` | push/PR | `mvn -B clean verify` |
| `integration` | push/PR | `mvn -B verify -Pit` |
| `frontend` | push/PR | `npm ci` + lint + build em `periscope-ui` |
| `dependency-check` | push/PR | OWASP (não bloqueante) |
| `e2e` | `workflow_dispatch` | Playwright (WildFly+Mongo; desabilitado no push) |

Template inerte de homologação: [`.github/workflows/deploy-staging.yml`](.github/workflows/deploy-staging.yml).

## Context root

`/periscope` (definido em `WEB-INF/jboss-web.xml`).

## Documentação

| Documento | Conteúdo |
|-----------|----------|
| [CHANGELOG.md](CHANGELOG.md) | Histórico de mudanças (Keep a Changelog) |
| [docs/modernization/README.md](docs/modernization/README.md) | Plano de modernização (fases 0–8) |
| [docs/modernization/fase-07-spa.md](docs/modernization/fase-07-spa.md) | API REST + SPA |
| [docs/modernization/fase-08-qualidade-ci.md](docs/modernization/fase-08-qualidade-ci.md) | CI, testes, OpenAPI, E2E |
| [AGENTS.md](AGENTS.md) | Instruções para agentes / ambiente Cloud |
