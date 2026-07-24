# Inventário técnico

## Módulos Maven

| Módulo | Packaging | Papel |
|--------|-----------|-------|
| `periscope` (pai) | `pom` | BOM legado Java EE 6, compiler 1.6 |
| `periscope-ejb` | `ejb` | Modelos, Morphia, Lucene, importadores, seed |
| `periscope-web` | `war` | JSF/PrimeFaces, controllers, filters |
| `periscope-ear` | `ear` | Empacota WAR (EJB embutido no WAR); removido na Fase 6 |

## Dependências diretas relevantes

### `periscope-ejb`

| Artefato | Versão atual | Destino |
|----------|--------------|---------|
| Morphia (`com.github.jmkgreen.morphia`) | 1.2.3 | Morphia 2.4 (Fase 3) |
| Mongo driver (transitivo) | 2.x | Driver 5.x (Fase 3) |
| Lucene core/analyzers/queryparser/queries | 6.0.0 | Lucene 9 (Fase 4) |
| POI | 3.10-beta2 | POI 5 (Fase 5) |
| PDFBox / fontbox / jempbox | 1.8.4 | PDFBox 3 (Fase 5) |
| PrimeFaces | 3.4.2 | Mover para web + PF 14 (Fases 1–2/5) |
| fixjures | 2.0-SNAPSHOT | Jackson YAML (Fase 1) |
| `javax:javaee-api` | 6.0 | Jakarta EE 10 BOM (Fase 2) |

### `periscope-web`

| Artefato | Versão atual | Destino |
|----------|--------------|---------|
| commons-io (groupId errado) | 1.3.2 | `commons-io:commons-io:2.16.1` (Fase 1) |
| PrimeFaces theme bootstrap | 1.0.8 | Avaliar com PF 14 |
| commons-fileupload | 1.3 | Atualizar com upload PF |
| Specs JSF/CDI/JAX-RS JBoss | EE 6 | Provided via Jakarta EE 10 |

## Repositórios Maven problemáticos

| ID | URL | Ação |
|----|-----|------|
| `fixjures` | `http://fixjures.googlecode.com/svn/repo` | Remover (Fase 1) |
| `prime-repo` | `http://repository.primefaces.org` | Remover (Fase 1) |

## Seed / recursos

YAML em `periscope-ejb/src/main/resources/`:

- `user-inicial.yaml`
- `country-inicial-data.yaml`
- `applicantType-inicial.yaml`
- `descriptors.yaml`

Scripts JS de similaridade: `js/longestCommonSubstring.js`, `js/levenshtein.js`, `js/liquidmetal.js` (carregados em `system.js` do MongoDB por `SeedBean`).

## Descriptors

| Arquivo | Conteúdo |
|---------|----------|
| `periscope-web/.../WEB-INF/web.xml` | FacesServlet, FileUploadFilter, theme |
| `faces-config.xml` | Navegação + i18n |
| `beans.xml` (web + ejb) | CDI 1.0; decorator no EJB |
| `jboss-web.xml` | context-root `/periscope-web` (EAR usa `/periscope`) |

## Contagem aproximada de código

- ~111 arquivos Java (73 EJB + 38 web)
- ~63 XHTML
- ~266 imports `javax.*` a migrar (exceto `javax.crypto` / etc.)
