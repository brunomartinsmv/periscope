# Modernização do Periscope

Plano de atualização tecnológica do sistema Periscope (UFMT), dividido em fases independentes. Cada fase possui um PR de planejamento com escopo, tarefas, arquivos afetados e critérios de aceite.

## Decisões arquiteturais (fixadas)

| Decisão | Escolha |
|---------|---------|
| Linguagem | **Java 21 LTS** |
| Servidor de aplicação | **WildFly 31+** (Jakarta EE 10) |
| Persistência | **MongoDB 7** + driver moderno + **Morphia 2.4** |
| Frontend | **SPA** (API REST + frontend separado) |
| Empacotamento | **WAR único** (eliminar módulo EAR) |
| Homologação | **Não disponível hoje** — Fase 0 prepara baseline local; Fase 8 prepara ambiente futuro |

## Estado atual (baseline)

- Java 1.6, Java EE 6, JBoss AS 7
- JSF 2.0 + PrimeFaces 3.4.2
- Morphia 1.2.3 + MongoDB driver 2.x (API legada)
- Lucene 6.0.0 (harmonização Fast-Join)
- EAR multi-módulo (`periscope-ejb`, `periscope-web`, `periscope-ear`)
- Build quebrado (repositórios HTTP mortos, fixjures indisponível)

## Fases e PRs

| Fase | PR | Documento | Objetivo |
|------|----|-----------|----------|
| 0 | Preparação | [fase-00-preparacao.md](fase-00-preparacao.md) | Baseline, inventário, ambiente local |
| 1 | Build | [fase-01-build.md](fase-01-build.md) | Compilar com Java 21 e Maven moderno |
| 2 | Jakarta EE | [fase-02-jakarta-ee.md](fase-02-jakarta-ee.md) | `javax.*` → `jakarta.*`, CDI/JSF moderno |
| 3 | MongoDB | [fase-03-mongodb.md](fase-03-mongodb.md) | Driver 4.x/5.x, Morphia 2, GridFS, agregações |
| 4 | Lucene | [fase-04-lucene.md](fase-04-lucene.md) | Lucene 9.x, harmonização Fast-Join |
| 5 | Bibliotecas | [fase-05-bibliotecas.md](fase-05-bibliotecas.md) | PDFBox 3, POI 5, PrimeFaces 14 |
| 6 | Deploy | [fase-06-war-wildfly.md](fase-06-war-wildfly.md) | WAR único, WildFly, Docker Compose |
| 7 | SPA | [fase-07-spa.md](fase-07-spa.md) | API REST + frontend SPA |
| 8 | Qualidade | [fase-08-qualidade-ci.md](fase-08-qualidade-ci.md) | Testes, CI/CD, homologação futura |

## Ordem de execução

```
Fase 0 → Fase 1 → Fase 2 → Fase 3 ─┐
                         Fase 4 ──┤→ Fase 5 → Fase 6 → Fase 7
                                   └→ Fase 8 (paralelo a partir da Fase 6)
```

- **Fases 3 e 4** podem ser feitas em paralelo após a Fase 2.
- **Fase 7 (SPA)** depende da Fase 6 (API deployável) e avança enquanto JSF ainda funciona como fallback.
- **Fase 8** inicia cedo (CI básico na Fase 1) e expande até homologação futura.

## Como usar estes PRs

Cada PR contém **apenas documentação de planejamento** — nenhuma alteração de código de produção. Após revisar e mergear:

1. Abra o documento da fase correspondente.
2. Siga as tarefas na ordem indicada.
3. Marque critérios de aceite antes de avançar para a próxima fase.
