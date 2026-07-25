# Changelog

Todas as mudanças notáveis deste projeto são documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Pilha de modernização (PRs #12–#16) sobre o estado pós-PR #11 (Java 21, Jakarta EE 10,
Morphia 2, Lucene 9, PDFBox 3, POI 5, PrimeFaces 14).

### Added

- Empacotamento **WAR único** (`periscope-web/target/periscope.war`) com o EJB embarcado,
  context-root `/periscope`, `Dockerfile` multi-stage e `docker-compose.yml` (MongoDB 7 +
  WildFly 34) — PRs #12.
- Scripts `scripts/dev-up.sh` / `scripts/dev-down.sh` e documentação operacional atualizada
  (`README.md`, `AGENTS.md`, `docs/modernization/fase-06-war-wildfly.md`).
- Pipeline CI (`.github/workflows/ci.yml`): build Maven, perfil IT com Testcontainers,
  lint/build do frontend, OWASP dependency-check (não bloqueante) e job E2E sob
  `workflow_dispatch` — PRs #13 / #15.
- Template inerte de deploy de homologação (`.github/workflows/deploy-staging.yml`).
- Suíte de testes unitários JUnit 5 + Mockito + AssertJ (~37 testes): importadores
  Espacenet/DPMA/Patentscope, `PDFTextParser`, `YamlLoader`, Fast-Join, JWT e DTOs;
  JaCoCo e `.editorconfig` — PR #13.
- Testes de integração com Testcontainers MongoDB (`PatentRepositoryIT`,
  `ReferenceCycleIT`) no perfil Maven `it`.
- Health check `GET /periscope/rest/health` (MongoDB + índice Lucene).
- **API REST** JWT em `/periscope/rest`: autenticação, projetos, patentes, importação
  multipart, harmonização (sugestões Fast-Join e regras), relatórios, usuários e
  arquivos GridFS — PR #14.
- SPA **React + Vite + TypeScript** (`periscope-ui/`) servida em `/periscope/app/`, com
  login, projetos, patentes, importação, harmonização, relatórios e usuários; JSF
  permanece como fallback — PR #14.
- Documentação OpenAPI 3 em `/openapi` (MicroProfile OpenAPI / SmallRye no WildFly) e
  script `tools/enable-wildfly-openapi.sh` — PR #15.
- Testes E2E Playwright em `periscope-ui/e2e/` (login, projetos, patentes, relatórios,
  harmonização).

### Changed

- Servidor alvo de **WildFly 8 / EAR** para **WildFly 34.0.1.Final** e WAR único.
- Morphia **2.4.14 → 2.5.3** (obrigatório com bson 5.x) e Jackson alinhado a **2.17.0**
  (mesma linha do WildFly 34).
- Login JSF e API REST passam a autenticar via `UserAuthenticator` / `PasswordHasher`
  (PBKDF2), com migração transparente da senha em texto plano do seed.
- Gráficos da UI JSF migrados para a API Chart.js do PrimeFaces 14 (`BarChartModel`,
  `LineChartModel`, `DefaultTagCloudModel`); removido o shim
  `br.ufmt.periscope.compat.chart` — PR #16.
- Expressões EL `p:component` (removidas no PrimeFaces 14) substituídas por
  `p:resolveFirstComponentWithId` nas telas de harmonização, patentes e no filtro de
  relatórios.

### Fixed

- Ciclo de `@Reference` Morphia (`Project.patents` ↔ `Patent.project`) que esgotava o
  pool de conexões do MongoDB ao abrir um projeto na UI JSF; coleções cíclicas passam a
  ser `@Reference(lazy = true)` — PR #15.
- Projeções de projeto que apagavam silenciosamente `patents`/`rules` ao salvar via API.
- Listagem paginada de patentes que resolvia a referência `project` linha a linha e
  estourava o pool (timeout de 120s).
- JSON malformado e campos desconhecidos no payload da API, que devolviam HTTP 500;
  agora 400 / aceitos com `FAIL_ON_UNKNOWN_PROPERTIES=false`.
- Upload PrimeFaces nativo (`primefaces.UPLOADER=native`) sem `<multipart-config>` no
  Faces Servlet: `FileUploadEvent` nunca disparava. Adicionado `multipart-config` e
  `BufferedUploadedFile` (o `Part` nativo é request-scoped) — PR #16.
- Harmonização: update usava `applicants.$.nature` (campo inexistente; o modelo usa
  `type`); filtro tipado por projeto e indexação da regra sem `TextField` no `id`.
- `RuleDTO.projectId` deixava de vir preenchido nas listagens.

### Security

- Senhas armazenadas com **PBKDF2-HMAC-SHA256** (≥120 000 iterações, salt aleatório);
  seed `admin`/`123456` é rehashado no primeiro login bem-sucedido.
- API REST protegida por JWT HS256 (`Authorization: Bearer`) e `@RolesAllowed`; CORS
  configurável via `PERISCOPE_CORS_ORIGINS`.

### Removed

- Módulo Maven `periscope-ear` e empacotamento EAR.
- Shim de gráficos `br.ufmt.periscope.compat.chart` (substituído pela API Chart.js do
  PrimeFaces 14).

## [1.0.0] — 2026-07-24

Baseline pós-Fases 0–5 (parcial), mergeada via PR #11 e PRs anteriores (#1, #4, #5, #10).

### Added

- Compilação e runtime em **Java 21** / **Jakarta EE 10** / PrimeFaces 14 (`jakarta`).
- Persistência com **Morphia 2** + **mongodb-driver-sync 5.2.1** (sem JPA/SQL).
- Harmonização Fast-Join em **Apache Lucene 9.12.0**.
- Extração de PDF com **PDFBox 3.0.3** e importadores com **Apache POI 5.3.0**.
- Ambiente local documentado e seed inicial (`admin` / `123456`) via `SeedBean`.

### Changed

- Migração completa `javax.*` → `jakarta.*` e beans gerenciados para CDI.
- Build Maven moderno (`maven.compiler.release=21`), sem repositórios `http://` bloqueados.

[Unreleased]: https://github.com/brunomartinsmv/periscope/compare/95a500c...bruno/fase-5-primefaces-jsf-fixes-a4b3
[1.0.0]: https://github.com/brunomartinsmv/periscope/tree/95a500c
