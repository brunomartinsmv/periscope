# Homologação futura

Não há ambiente de homologação hoje. Requisitos quando existir:

## Infraestrutura

- [ ] MongoDB dedicado com snapshot de dados **anonimizados**
- [ ] WildFly **34** com WAR único `periscope.war` e variáveis:
  - `MONGODB_URI`
  - `MONGODB_DATABASE` (default `Periscope`)
  - `PERISCOPE_DIR` (índice Lucene 9)
  - `PERISCOPE_JWT_SECRET` (obrigatório em staging/prod)
- [ ] Subsystem MicroProfile OpenAPI habilitado (`tools/enable-wildfly-openapi.sh`)
- [ ] Rede/segredos gerenciados (sem credenciais no repositório)
- [ ] Context-root `/periscope` e HTTPS definidos pelo ambiente
- [ ] Healthcheck HTTP: `GET /periscope/rest/health` → `200` e `"status":"UP"`
- [ ] OpenAPI: `GET /openapi` → documento válido

## Pipeline

- [x] CI GitHub Actions com build Maven (JDK 21) — `.github/workflows/ci.yml`
- [x] Job de integração com Testcontainers (`-Pit`)
- [x] Job frontend (`npm ci` / lint / build) — Fase 8b
- [x] Job E2E Playwright sob `workflow_dispatch` (desabilitado no push padrão)
- [x] Template inerte de deploy: [`.github/workflows/deploy-staging.yml`](../../.github/workflows/deploy-staging.yml)
  - Trigger apenas `workflow_dispatch` + `if: false` no job
  - Passos planejados: build imagem → push registry → deploy → smoke `/rest/health` → E2E com `E2E_BASE_URL`
- [ ] Deploy automático para homologação após merge em branch de release (ativar o template)
- [ ] Marcadores de deploy (`*.deployed` / healthcheck HTTP acima)
- [ ] Rollback documentado

## Qualidade

- [x] Testes unitários + IT Morphia/Mongo (Fase 8a)
- [x] Testes E2E Playwright contra ambiente local / staging futuro (Fase 8b)
- [ ] Smoke checklist operacional (login, projetos, importação, harmonização, relatório)
- [x] Dataset de teste versionado — `docs/modernization/dataset-teste/`

## Observabilidade

- [ ] Logs centralizados do WildFly
- [ ] Métricas básicas de disponibilidade (health + MongoDB)
- [ ] OpenAPI publicado para consumidores da API

## Relação com as fases

- Fase 0: documenta necessidade (este arquivo)
- Fase 6: empacota WAR único + Compose (base para homologação)
- Fase 7: API REST + SPA
- Fase 8a: CI, testes Maven, health endpoint
- Fase 8b: OpenAPI, E2E, CI frontend, template `deploy-staging.yml`
