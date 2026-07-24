# Modernização do Periscope

Plano de atualização tecnológica do sistema Periscope (UFMT), dividido em fases independentes. Cada fase possui documentação de escopo, tarefas, arquivos afetados e critérios de aceite.

## Decisões arquiteturais (fechadas)

| Decisão | Escolha |
|---------|---------|
| Linguagem | **Java 21 LTS** |
| Servidor de aplicação | **WildFly 34** (Jakarta EE 10) |
| Persistência | **MongoDB 7** (local smoke: 4.4+) + driver 5.x + **Morphia 2.5** |
| Frontend | **SPA React** (`periscope-ui`) + API REST JWT — JSF/PrimeFaces como fallback |
| Empacotamento | **WAR único** (`periscope-web` + EJB embarcado; EAR removido) |
| Homologação | **Não disponível hoje** — Docker Compose na Fase 6; template de deploy na Fase 8b |

## Estado atual (baseline)

- **WAR único** `periscope-web/target/periscope.war`, context-root `/periscope`, WildFly 34 (JDK 21)
- **API REST** em `/periscope/rest` com JWT (`/auth`, projects, patents, import, harmonization, reports, users, files, health)
- **OpenAPI 3** em `/openapi` (MicroProfile OpenAPI / SmallRye no WildFly)
- **SPA React** em `/periscope/app/` (Vite + TS; `npm run build:war`) + JSF legado
- **CI** `.github/workflows/ci.yml`: Maven verify, IT `-Pit`, frontend lint/build, dependency-check; E2E Playwright sob `workflow_dispatch`
- **Testes:** ~32 unitários Maven + IT Testcontainers (perfil `it`) + Playwright E2E em `periscope-ui/e2e/`

## Fases e documentos

| Fase | Documento | Objetivo | Estado |
|------|-----------|----------|--------|
| 0 | [fase-00-preparacao.md](fase-00-preparacao.md) | Baseline, inventário, ambiente local | Feita |
| 1 | [fase-01-build.md](fase-01-build.md) | Compilar com Java 21 e Maven moderno | Feita |
| 2 | [fase-02-jakarta-ee.md](fase-02-jakarta-ee.md) | `javax.*` → `jakarta.*`, CDI/JSF moderno | Feita |
| 3 | [fase-03-mongodb.md](fase-03-mongodb.md) | Driver 4.x/5.x, Morphia 2, GridFS, agregações | Feita |
| 4 | [fase-04-lucene.md](fase-04-lucene.md) | Lucene 9.x, harmonização Fast-Join | Feita |
| 5 | [fase-05-bibliotecas.md](fase-05-bibliotecas.md) | PDFBox 3, POI 5, PrimeFaces 14 | Feita |
| 6 | [fase-06-war-wildfly.md](fase-06-war-wildfly.md) | WAR único, WildFly 34, Docker Compose | **Implementada** |
| 7 | [fase-07-spa.md](fase-07-spa.md) | API REST + SPA React | **Implementada** |
| 8a / 8b | [fase-08-qualidade-ci.md](fase-08-qualidade-ci.md) | CI, testes, OpenAPI, E2E, health | **Implementada** |

Homologação futura: [ambiente-homologacao-futuro.md](ambiente-homologacao-futuro.md).

## Ordem de execução

```
Fase 0 → Fase 1 → Fase 2 → Fase 3 ─┐
                         Fase 4 ──┤→ Fase 5 → Fase 6 → Fase 7
                                   └→ Fase 8 (paralelo a partir da Fase 6)
```

- **Fases 3 e 4** podem ser feitas em paralelo após a Fase 2.
- **Fase 7 (SPA)** depende da Fase 6 (WAR deployável) e mantém JSF como fallback.
- **Fase 8** inicia cedo (CI básico) e fecha com OpenAPI + E2E + template de staging.

## Como usar estes documentos

1. Abra o documento da fase correspondente.
2. Siga as tarefas na ordem indicada.
3. Marque critérios de aceite antes de avançar para a próxima fase.
