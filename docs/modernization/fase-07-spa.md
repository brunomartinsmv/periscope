# Fase 7 — API REST para SPA

**Branch de implementação (backend):** `bruno/fase-7-spa-a4b3`  
**Depende de:** Fase 6 (WAR / WildFly 34)  
**Frontend:** projeto `periscope-ui` (outro agente) — **não** faz parte deste entregável.

## Objetivo

Expor uma **API REST JSON** sob `/periscope/rest/*` para a futura SPA, mantendo JSF/PrimeFaces como UI legada durante a transição.

## Decisões aplicadas

| Tema | Decisão |
|------|----------|
| Empacotamento | WAR único (`periscope-web`); sem EAR |
| Auth SPA | JWT HS256 (sem lib JWT externa; `Mac` + Base64 URL-safe) |
| Senhas | PBKDF2-HMAC-SHA256 (`pbkdf2$iter$saltB64$hashB64`), migração transparente do texto plano |
| JSON | Jackson 2.17 embarcado no WAR (`jackson-jakarta-rs-json-provider`); módulos Jackson do servidor continuam excluídos |
| CORS | `PERISCOPE_CORS_ORIGINS` (default `http://localhost:5173`) |
| Roles | `@RolesAllowed` + `resteasy.role.based.security=true` |

## Pacotes

```
periscope-web/.../api/
├── dto/          # records DTO (nunca entidades Morphia)
├── resource/     # JAX-RS resources
├── security/     # JWT, AuthService, JwtAuthFilter
└── filter/       # CORS, ExceptionMapper
periscope-ejb/.../security/
├── PasswordHasher.java
└── UserAuthenticator.java
```

`HealthResource` permanece em `br.ufmt.periscope.rest` → `GET /periscope/rest/health`.

## Autenticação

1. `POST /rest/auth/login` com `{ "username", "password" }` → `{ "token", "user" }`
2. Cliente envia `Authorization: Bearer <token>`
3. `JwtAuthFilter` valida assinatura/expiração e popula `SecurityContext` (`ADMIN` / `USER`)
4. Endpoints públicos (sem Bearer): `POST /auth/login`, `GET /health`, `OPTIONS` (preflight)
5. `POST /auth/logout` é **stateless** (204; o cliente descarta o token)

Claims JWT: `sub`, `userLevel`, `iat`, `exp` (default 8h; `PERISCOPE_JWT_EXPIRATION_HOURS`).

Segredo: `PERISCOPE_JWT_SECRET` (se ausente, segredo efêmero em memória + log de aviso).

### Migração de senha

Login JSF (`SessionBean`) e API usam o mesmo `UserAuthenticator`:

- valor sem prefixo `pbkdf2$` → compara texto plano; se OK, regrava hash
- seed `admin`/`123456` continua válido na primeira autenticação

## Endpoints implementados

Base: `http://localhost:8080/periscope/rest`

| Método | Path | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/health` | público | Liveness (Mongo + Lucene) |
| POST | `/auth/login` | público | Emite JWT |
| GET | `/auth/me` | Bearer | Usuário atual |
| POST | `/auth/logout` | Bearer | 204 stateless |
| GET | `/projects` | Bearer | Lista projetos do usuário |
| POST | `/projects` | Bearer | Cria projeto |
| GET | `/projects/{id}` | Bearer | Detalhe |
| PUT | `/projects/{id}` | Bearer | Atualiza |
| DELETE | `/projects/{id}` | Bearer | Remove (owner/admin) |
| GET | `/projects/{id}/patents?page&size&q&country` | Bearer | Patentes paginadas (`ProjectPatentResource`; `projectId` vem do path, sem resolver `@Reference project`) |
| POST | `/projects/{id}/patents/import` | Bearer | multipart: `file` + `importer` |
| GET/PUT/DELETE | `/patents/{id}` | Bearer | CRUD patente (`projectId` no DTO; listagem **não** resolve `@Reference project`) |
| GET | `/projects/{id}/harmonization/suggestions?type&query` | Bearer | Lucene Fast-Join |
| GET/POST | `/projects/{id}/harmonization/rules` | Bearer | Regras |
| DELETE | `/projects/{id}/harmonization/rules/{ruleId}` | Bearer | Remove regra |
| POST | `/projects/{id}/harmonization/apply` | Bearer | Aplica todas |
| POST | `/projects/{id}/harmonization/rules/{ruleId}/apply` | Bearer | Aplica uma |
| GET | `/projects/{id}/reports/main-applicant` | Bearer | Relatório |
| GET | `/projects/{id}/reports/main-inventor` | Bearer | Relatório |
| GET | `/projects/{id}/reports/main-ipc` | Bearer | Relatório |
| GET | `/projects/{id}/reports/application-date` | Bearer | Relatório |
| GET | `/projects/{id}/reports/publication-date` | Bearer | Relatório |
| GET/POST/PUT/DELETE | `/users`… | ADMIN | CRUD usuários (sem hash na resposta) |
| GET | `/files/{id}` | Bearer | Download GridFS |
| POST | `/files/patents/{patentId}?kind=` | Bearer | Upload anexo |

Erros: JSON `{"error":"...","status":n}` (sem stacktrace).
JSON malformado / corpo ilegível → **400**. Campos desconhecidos no payload são **ignorados** (`FAIL_ON_UNKNOWN_PROPERTIES=false`).

`PatentDTO.projectId`: na listagem paginada o valor vem do path (`/projects/{id}/patents`), não de uma resolução Morphia de `@Reference` (evita esgotar o pool Mongo).

## Variáveis de ambiente

| Variável | Default | Uso |
|----------|---------|-----|
| `PERISCOPE_JWT_SECRET` | (efêmero) | Assinatura HS256 |
| `PERISCOPE_JWT_EXPIRATION_HOURS` | `8` | Validade do token |
| `PERISCOPE_CORS_ORIGINS` | `http://localhost:5173` | Origins CORS (CSV) |

## Frontend (`periscope-ui`)

SPA React implementada em `periscope-ui/` (branch `bruno/fase-7-spa-a4b3`).

### Stack

| Peça | Escolha |
|------|---------|
| UI | React 19 + TypeScript |
| Bundler | Vite 8 (`base: '/periscope/app/'`) |
| Rotas | `react-router-dom` (basename `/periscope/app`) |
| Dados | `@tanstack/react-query` |
| HTTP | `fetch` encapsulado em `src/api/client.ts` |
| Gráficos | `recharts` |
| Estilo | CSS próprio (`src/styles/global.css`) |

### Rotas implementadas

| Rota SPA | Endpoints consumidos |
|----------|----------------------|
| `/login` | `POST /auth/login`, `GET /auth/me`, `POST /auth/logout` |
| `/projects` | `GET/POST /projects`, `PUT/DELETE /projects/{id}` |
| `/projects/:id/patents` | `GET /projects/{id}/patents?page&size`, `DELETE /patents/{id}` |
| `/projects/:id/patents/:patentId` | `GET/PUT/DELETE /patents/{id}`, `GET /files/{id}` |
| `/projects/:id/import` | `POST /projects/{id}/patents/import` (multipart `file` + `type`) |
| `/projects/:id/harmonization` | `GET …/suggestions`, `GET/POST/DELETE …/rules`, `POST …/apply` |
| `/projects/:id/reports` | `GET …/reports/{main-applicant\|main-inventor\|main-ipc\|application-date\|publication-date}` |
| `/users` | `GET /users` (menu só para `ADMIN`) |

Auth: JWT em `localStorage`, header `Authorization: Bearer`, 401 limpa sessão e redireciona para `/login`.

### Como servir

**Dev:** `cd periscope-ui && npm ci && npm run dev` → `http://localhost:5173/periscope/app/`  
Proxy Vite: `/periscope/rest` → `http://localhost:8080`. Variável `VITE_API_BASE` (default `/periscope/rest`).

**Produção (WAR):** sem Node no Maven. Gere os estáticos e empacote:

```bash
cd periscope-ui && npm run build:war
# → periscope-web/src/main/webapp/app/  (gitignored)
mvn -B package
```

`SpaFallbackFilter` (`/app/*`) devolve `index.html` para rotas client-side, sem afetar `*.jsf`, `/rest/*` nem `/pages/*`. Sem a pasta `app/`, o WAR sobe só com JSF + REST.

Alternativa: nginx servindo `dist/` com `try_files` + proxy da API — não usado neste ambiente.

## Testes

Unitários (sem container): `PasswordHasherTest`, `JwtServiceTest`, `AuthServiceTest`,
`DtoMappingTest`, `JacksonObjectMapperProviderTest`.

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -B clean verify
```
