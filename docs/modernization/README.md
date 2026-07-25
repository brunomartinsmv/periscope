# Modernização do Periscope

Plano de atualização tecnológica do sistema Periscope (UFMT), dividido em fases independentes. Cada fase possui documentação de escopo, tarefas, arquivos afetados e critérios de aceite.

## Decisões arquiteturais (fixadas)

| Decisão | Escolha |
|---------|---------|
| Linguagem | **Java 21 LTS** |
| Servidor de aplicação | **WildFly 34** (Jakarta EE 10) |
| Persistência | **MongoDB 7** (local smoke: 4.4+) + driver 5.x + **Morphia 2.4** |
| Frontend | **SPA** (API REST + frontend separado) — Fase 7 |
| Empacotamento | **WAR único** (`periscope-web` + EJB embarcado; EAR removido) |
| Homologação | **Não disponível hoje** — Docker Compose na Fase 6; Fase 8 prepara ambiente futuro |

## Estado atual

- Java 21, Jakarta EE 10, PrimeFaces 14 (Fases 0–2)
- Morphia 2.5.3 + mongodb-driver-sync 5.2.1 (Fase 3; Morphia alinhado a 2.5 na Fase 6 para driver 5.x)
- Lucene 9.12.0 (harmonização Fast-Join; Fase 4)
- PDFBox 3.0.3, POI 5.3.0, upload nativo PrimeFaces (Fase 5)
- **WAR único** `periscope-web/target/periscope.war`, WildFly 34, Docker Compose (Fase 6 — **concluída**)

## Fases e documentos

| Fase | Documento | Objetivo | Estado |
|------|-----------|----------|--------|
| 0 | [fase-00-preparacao.md](fase-00-preparacao.md) | Baseline, inventário, ambiente local | Feita |
| 1 | [fase-01-build.md](fase-01-build.md) | Compilar com Java 21 e Maven moderno | Feita |
| 2 | [fase-02-jakarta-ee.md](fase-02-jakarta-ee.md) | `javax.*` → `jakarta.*`, CDI/JSF moderno | Feita |
| 3 | [fase-03-mongodb.md](fase-03-mongodb.md) | Driver 4.x/5.x, Morphia 2, GridFS, agregações | Feita |
| 4 | [fase-04-lucene.md](fase-04-lucene.md) | Lucene 9.x, harmonização Fast-Join | Feita |
| 5 | [fase-05-bibliotecas.md](fase-05-bibliotecas.md) | PDFBox 3, POI 5, PrimeFaces 14 | Feita |
| 6 | [fase-06-war-wildfly.md](fase-06-war-wildfly.md) | WAR único, WildFly 34, Docker Compose | **Concluída** |
| 7 | fase-07-spa.md | API REST + frontend SPA | Pendente (doc ainda não criado) |
| 8 | fase-08-qualidade-ci.md | Testes, CI/CD, homologação futura | Pendente (doc ainda não criado) |

> **Nota:** os links das Fases 7 e 8 ficam intencionalmente sem arquivo até as etapas correspondentes. A Fase 6 já possui documento de planejamento/aceitação em `fase-06-war-wildfly.md`.

## Ordem de execução

```
Fase 0 → Fase 1 → Fase 2 → Fase 3 ─┐
                         Fase 4 ──┤→ Fase 5 → Fase 6 → Fase 7
                                   └→ Fase 8 (paralelo a partir da Fase 6)
```

- **Fases 3 e 4** podem ser feitas em paralelo após a Fase 2.
- **Fase 7 (SPA)** depende da Fase 6 (WAR deployável) e avança enquanto JSF ainda funciona como fallback.
- **Fase 8** inicia cedo (CI básico) e expande até homologação futura.

## Como usar estes documentos

1. Abra o documento da fase correspondente.
2. Siga as tarefas na ordem indicada.
3. Marque critérios de aceite antes de avançar para a próxima fase.
