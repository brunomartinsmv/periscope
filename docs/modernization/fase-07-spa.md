# Fase 7 — Migração para SPA

**PR:** `cursor/fase-7-spa-8905`  
**Depende de:** Fase 6  
**Bloqueia:** nada (pode ser incremental)

## Objetivo

Substituir gradualmente **JSF/PrimeFaces/XHTML** por uma **SPA moderna** consumindo **API REST** exposta pelo backend Jakarta EE no WildFly.

---

## Decisões aplicadas

- Backend permanece Java 21 + WildFly + EJB/CDI
- Frontend SPA separado (projeto/build independente)
- JSF mantido como fallback durante transição
- API REST como contrato estável entre frontend e backend

---

## Arquitetura alvo

```
┌─────────────────┐     REST/JSON      ┌──────────────────────────┐
│   SPA Frontend  │ ◄────────────────► │  periscope.war (WildFly)  │
│  (React/Vue/    │                    │  ├── JAX-RS Resources     │
│   Angular)      │                    │  ├── EJB Services         │
└─────────────────┘                    │  └── MongoDB + Lucene     │
        ▲                              └──────────────────────────┘
        │ static assets
   nginx / WildFly
```

---

## Tarefas

### 7.1 Escolher stack frontend

| Opção | Prós | Contras |
|-------|------|---------|
| **React + Vite + TypeScript** | Ecossistema grande, hiring | Mais boilerplate |
| **Vue 3 + Vite + TypeScript** | Curva suave, SFC | Menor ecossistema enterprise |
| **Angular 19** | Opinativo, enterprise | Mais pesado |

**Recomendação:** React + Vite + TypeScript + TanStack Query + React Router.

**Justificativa:** melhor equilíbrio entre comunidade, tipagem e velocidade de migração incremental.

### 7.2 Estrutura do projeto frontend

**Criar:** `periscope-ui/` na raiz do repositório (monorepo) ou repositório separado.

```
periscope-ui/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── src/
│   ├── api/           # Cliente HTTP (axios/fetch)
│   ├── components/    # Componentes reutilizáveis
│   ├── pages/         # Telas (mapeamento 1:1 com XHTML)
│   ├── hooks/         # Custom hooks
│   ├── types/         # DTOs TypeScript
│   └── main.tsx
└── public/
```

**Atualizar POM pai** (opcional): não incluir frontend no Maven; build separado via npm.

### 7.3 Expandir API REST (JAX-RS)

**Base existente:** `JaxRsActivator.java` — verificar `@ApplicationPath`.

**Criar resources REST:**

| Resource | Endpoints | Substitui |
|----------|-----------|-----------|
| `AuthResource` | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` | `LoginController` |
| `ProjectResource` | CRUD `/api/projects` | `ProjectController` |
| `PatentResource` | CRUD + import `/api/projects/{id}/patents` | `PatentController`, `ImportPatentController` |
| `HarmonizationResource` | `/api/projects/{id}/harmonization/*` | Controllers harmonization |
| `ReportResource` | `/api/projects/{id}/reports/*` | Controllers de relatório |
| `UserResource` | CRUD `/api/users` | `UserController` |
| `FileResource` | Upload/download `/api/files/*` | GridFS via REST |

**Padrão de implementação:**

```java
@Path("/api/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ProjectResource {
    @Inject ProjectRepository repository;

    @GET
    public List<ProjectDTO> list() { ... }

    @POST
    public Response create(ProjectDTO dto) { ... }
}
```

**DTOs:** criar pacote `br.ufmt.periscope.api.dto` — não expor entidades Morphia diretamente.

### 7.4 Autenticação e segurança

**Opções:**

| Abordagem | Complexidade | Recomendação |
|-----------|--------------|--------------|
| JWT stateless | Média | **Recomendado** para SPA |
| Session cookie (JSESSIONID) | Baixa | Funciona, acoplado ao WildFly |
| OAuth2/OIDC | Alta | Futuro, se integrar SSO institucional |

**Implementação JWT sugerida:**

1. `POST /api/auth/login` → retorna `{ token, user }`
2. SPA armazena token (httpOnly cookie ou localStorage)
3. Filter JAX-RS valida `Authorization: Bearer <token>`
4. `@RolesAllowed` nos endpoints

**Criar:** `JwtAuthFilter.java`, `AuthService.java`

### 7.5 CORS

SPA em dev roda em `localhost:5173`; API em `localhost:8080`.

```java
@Provider
public class CorsFilter implements ContainerResponseFilter {
    // Access-Control-Allow-Origin, Methods, Headers
}
```

Em produção: servir SPA e API no mesmo domínio via reverse proxy.

### 7.6 Mapeamento telas JSF → SPA

Migrar **incrementalmente** por módulo:

| Prioridade | Módulo JSF | Página SPA | Complexidade |
|------------|------------|------------|--------------|
| P0 | Login | `/login` | Baixa |
| P0 | Lista de projetos | `/projects` | Baixa |
| P1 | Importação de patentes | `/projects/:id/import` | Alta |
| P1 | Lista/edição patentes | `/projects/:id/patents` | Alta |
| P2 | Harmonização | `/projects/:id/harmonization` | Muito alta |
| P2 | Relatórios | `/projects/:id/reports/*` | Alta |
| P3 | Usuários | `/users` | Média |
| P3 | About | `/about` | Baixa |

**Estratégia:** feature flag ou rota WildFly — JSF continua em `/periscope/faces/*`, SPA em `/periscope/app/*`.

### 7.7 Servir SPA no WildFly

**Opção A — embarcar no WAR:**

```
periscope-web/src/main/webapp/app/   ← build output do Vite
```

**Opção B — nginx sidecar no Docker Compose** (recomendado):

```yaml
frontend:
  build: ./periscope-ui
  ports:
    - "3000:80"
  depends_on:
    - periscope
```

### 7.8 Relatórios na SPA

Relatórios JSF renderizam server-side. Na SPA:

| Relatório atual | Abordagem SPA |
|-----------------|---------------|
| Tabelas/gráficos | Chart.js ou Recharts consumindo `/api/reports/*` |
| Exportação PDF/Excel | Endpoint REST retorna blob |
| Filtros PrimeFaces | Componentes React equivalentes |

### 7.9 Harmonização na SPA

Funcionalidade mais complexa:

- API: `GET /api/projects/{id}/harmonization/suggestions?type=applicant&query=...`
- Backend reutiliza Lucene Fast-Join existente
- UI: autocomplete + tabela de regras + confirmação

### 7.10 Descomissionar JSF

**Somente após** todos os módulos migrados e validados:

- [ ] Remover XHTML (~70 arquivos)
- [ ] Remover controllers JSF
- [ ] Remover PrimeFaces do POM
- [ ] Remover `faces-config.xml`
- [ ] Reduzir WAR significativamente

---

## Arquivos a criar

| Arquivo/Diretório | Descrição |
|-------------------|-----------|
| `periscope-ui/` | Projeto frontend completo |
| `periscope-web/.../api/` | JAX-RS resources |
| `periscope-web/.../api/dto/` | DTOs |
| `periscope-web/.../api/filter/` | JWT, CORS |
| `periscope-ejb/.../service/` | Services extraídos dos controllers |
| `docker-compose.yml` | Adicionar serviço frontend |

## Critérios de aceite

- [ ] SPA com login funcional via API REST
- [ ] CRUD de projetos via SPA
- [ ] Importação de patentes via SPA
- [ ] Harmonização funcional via SPA (sugestões Fast-Join)
- [ ] Pelo menos 3 relatórios renderizados na SPA
- [ ] JSF ainda funciona como fallback (durante transição)
- [ ] CORS configurado para desenvolvimento local
- [ ] Documentação API (OpenAPI/Swagger)

## Riscos

| Risco | Mitigação |
|-------|-----------|
| Escopo inflado | Migrar módulo a módulo; não big-bang |
| Paridade funcional JSF vs SPA | Checklist por tela |
| Upload de arquivos grande | Multipart REST + progress bar |
| Relatórios complexos | Manter export server-side inicialmente |

## Validação local

- [ ] `npm run dev` (SPA) + `docker compose up` (API)
- [ ] Fluxo completo: login → projeto → importar → harmonizar → relatório
- [ ] Comparar resultados SPA vs JSF com mesmo dataset

## Homologação futura

- Deploy SPA como container separado ou embarcado no WAR
- Testes E2E (Playwright/Cypress) contra URL de homologação
- Contract tests entre SPA e API (Pact ou schema validation)
