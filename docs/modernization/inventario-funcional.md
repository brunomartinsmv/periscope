# Inventário funcional

Mapeamento dos fluxos críticos do Periscope e suas telas/componentes.

## Fluxos P0

| Fluxo | Telas / componentes | Classes |
|-------|---------------------|---------|
| Login / logout | `login.xhtml`, filtro de acesso | `LoginController`, `UserAccessFilter`, `SessionBean` |
| Gestão de projetos | `pages/project/projectList.xhtml`, `editProject.xhtml`, `projectHome.xhtml` | `ProjectController`, `ProjectSessionBean` |
| Importação de patentes | `pages/project/patent/patentImport.xhtml`, `patentAdd.xhtml`, `patentList.xhtml`, `patentEdit.xhtml` | `ImportPatentController`, `PatentController`, importadores EJB |
| Harmonização (Fast-Join) | `pages/project/harmonization/*` | `ApplicantHarmonizationController`, `InventorHarmonizationController`, `RuleController`, `Harmonization` (EJB/Lucene) |

## Fluxos P1

| Fluxo | Telas / componentes | Classes |
|-------|---------------------|---------|
| Relatórios fixos | `pages/project/report/allReports/*`, menus em `main/fixedReports.xhtml` | Controllers `*Controller` de relatório, repositórios de agregação |
| Upload/download de arquivos | formulários de patente / GridFS | `PatentController`, repositórios Morphia/GridFS |

## Fluxos P2

| Fluxo | Telas / componentes | Classes |
|-------|---------------------|---------|
| Gestão de usuários | `pages/user/userList.xhtml`, `editUser.xhtml` | `UserController` |
| Idioma / sessão UI | `template/menu.xhtml` | `LanguageBean`, `SidebarController` |

## Templates e navegação

- Layout: `template/template.xhtml`, `template/templateProject.xhtml`
- Menu / sidebar: `template/menu.xhtml`, `template/sidebar.xhtml`
- Navegação declarada em `WEB-INF/faces-config.xml` (outcomes → views)

## Observações

- Controllers usam JSF `@ManagedBean` + `@ViewScoped` (migrar para CDI na Fase 2).
- Persistência e regras de negócio ficam no módulo `periscope-ejb`.
- Não há API REST de negócio além do `JaxRsActivator` (preparação para Fase 7).
