# Fase 0 — Preparação

**PR:** `cursor/fase-0-preparacao-8905`  
**Depende de:** nada  
**Bloqueia:** todas as demais fases

## Objetivo

Estabelecer baseline seguro, inventário completo e ambiente local mínimo antes de qualquer migração de código.

## Contexto

Não há ambiente de homologação hoje. Esta fase garante que o desenvolvedor consiga validar localmente cada entrega futura, e documenta o que será necessário quando homologação existir.

---

## Tarefas

### 0.1 Inventário funcional

Documentar fluxos críticos e suas telas/endpoints:

| Fluxo | Telas / componentes | Prioridade |
|-------|---------------------|------------|
| Login / logout | `login.xhtml`, `LoginController`, `UserAccessFilter` | P0 |
| Gestão de projetos | `projectList.xhtml`, `ProjectController` | P0 |
| Importação de patentes | `patentImport.xhtml`, `ImportPatentController`, importadores EJB | P0 |
| Harmonização (Fast-Join) | `harmonization/*`, `Harmonization.java`, Lucene | P0 |
| Relatórios fixos | `report/allReports/*`, repositórios de agregação | P1 |
| Upload/download de arquivos | `PatentController`, GridFS | P1 |
| Gestão de usuários | `userList.xhtml`, `UserController` | P2 |

**Entregável:** `docs/modernization/inventario-funcional.md` (criar na implementação).

### 0.2 Inventário técnico

Mapear dependências diretas e indiretas:

```
periscope-ejb/pom.xml   → Morphia, Lucene, POI, PDFBox, PrimeFaces, fixjures
periscope-web/pom.xml   → PrimeFaces, commons-io, EJB
periscope-ear/pom.xml   → empacotamento EAR (será removido na Fase 6)
pom.xml (parent)        → Java EE 6 BOM, compiler 1.6
```

**Entregável:** `docs/modernization/inventario-tecnico.md`.

### 0.3 Baseline de dados de seed

Os YAMLs em `periscope-ejb/src/main/resources/` alimentam o `SeedBean`:

- `user-inicial.yaml`
- `country-inicial-data.yaml`
- `applicantType-inicial.yaml`
- `descriptors.yaml`

**Ação:** exportar/copiar estes arquivos e validar que carregam corretamente após substituir fixjures (Fase 1).

### 0.4 Ambiente local mínimo

Sem homologação, o desenvolvedor precisa de:

| Serviço | Versão alvo | Uso |
|---------|-------------|-----|
| JDK | 21 | Compilação e execução |
| Maven | 3.9+ | Build |
| MongoDB | 7.x | Persistência (Docker) |
| WildFly | 31+ | Servidor (Docker, Fase 6) |

**Entregável:** checklist de pré-requisitos em `docs/modernization/ambiente-local.md`.

### 0.5 Estratégia de validação sem homologação

Até existir ambiente de homologação:

1. **Testes automatizados** — prioridade na Fase 8; iniciar com smoke tests na Fase 1.
2. **Docker Compose local** — reproduzir stack completa (Fase 6).
3. **Dataset de referência** — conjunto pequeno de patentes de teste para harmonização e relatórios.
4. **Checklist manual** — roteiro de smoke test pós-deploy local.

**Entregável:** `docs/modernization/checklist-validacao-local.md`.

### 0.6 Preparação para homologação futura

Documentar requisitos para quando homologação existir:

- [ ] MongoDB dedicado com snapshot de dados anonimizados
- [ ] WildFly configurado via variáveis de ambiente (`MONGODB_URI`, `PERISCOPE_DIR`)
- [ ] Pipeline CI/CD com deploy automático para homologação
- [ ] Testes E2E executados contra homologação antes de produção

**Entregável:** seção em `docs/modernization/ambiente-homologacao-futuro.md`.

### 0.7 Branching e convenções

Definir convenções para as fases de implementação:

```
master                          → estável
cursor/fase-N-implementacao-8905 → implementação de cada fase
```

Commits: `feat(fase-N): descrição` ou `chore(fase-N): descrição`.

---

## Arquivos a criar (implementação)

| Arquivo | Descrição |
|---------|-----------|
| `docs/modernization/inventario-funcional.md` | Fluxos e telas |
| `docs/modernization/inventario-tecnico.md` | Dependências e módulos |
| `docs/modernization/ambiente-local.md` | Setup local |
| `docs/modernization/checklist-validacao-local.md` | Smoke tests manuais |
| `docs/modernization/ambiente-homologacao-futuro.md` | Requisitos futuros |
| `docs/modernization/dataset-teste/` | Patentes de exemplo (opcional) |

## Critérios de aceite

- [ ] Inventário funcional e técnico documentados
- [ ] YAMLs de seed preservados e referenciados
- [ ] Checklist de ambiente local definido
- [ ] Estratégia de validação sem homologação acordada
- [ ] Requisitos de homologação futura documentados
- [ ] Nenhuma alteração de código de produção nesta fase

## Riscos

| Risco | Mitigação |
|-------|-----------|
| Funcionalidades não documentadas | Revisar todos os XHTML e controllers |
| Dataset de teste insuficiente | Incluir casos de harmonização e importação multi-formato |

## Estimativa de esforço

Documentação e setup local — baixa invasividade, sem alteração de código.
