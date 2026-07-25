# Homologação futura

Não há ambiente de homologação hoje. Requisitos quando existir:

## Infraestrutura

- [ ] MongoDB dedicado com snapshot de dados **anonimizados**
- [ ] WildFly **34** com WAR único `periscope.war` e variáveis:
  - `MONGODB_URI`
  - `MONGODB_DATABASE` (default `Periscope`)
  - `PERISCOPE_DIR` (índice Lucene 9)
- [ ] Rede/segredos gerenciados (sem credenciais no repositório)
- [ ] Context-root `/periscope` e HTTPS definidos pelo ambiente
- [ ] Healthcheck HTTP: `GET /periscope/rest/health` → `200` e `"status":"UP"`

## Pipeline

- [x] CI GitHub Actions com build Maven (JDK 21) — Fase 8a (`.github/workflows/ci.yml`)
- [x] Job de integração com Testcontainers (`-Pit`)
- [ ] Deploy automático para homologação após merge em branch de release
- [ ] Marcadores de deploy (`*.deployed` / healthcheck HTTP acima)
- [ ] Rollback documentado

## Qualidade

- [x] Testes unitários + IT Morphia/Mongo (Fase 8a)
- [ ] Testes E2E contra homologação (Fase 8b)
- [ ] Smoke checklist (login, projetos, importação, harmonização, relatório)
- [x] Dataset de teste versionado — `docs/modernization/dataset-teste/`

## Observabilidade

- [ ] Logs centralizados do WildFly
- [ ] Métricas básicas de disponibilidade (health + MongoDB)

## Relação com as fases

- Fase 0: documenta necessidade (este arquivo)
- Fase 6: empacota WAR único + Compose (base para homologação)
- Fase 8a: CI, testes, health endpoint
- Fase 8b: OpenAPI, E2E, gates de cobertura
