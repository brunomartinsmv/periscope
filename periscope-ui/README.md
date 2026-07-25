# Periscope UI

SPA React (TypeScript) da Fase 7 do Periscope. Consome a API REST em `/periscope/rest`.

## Requisitos

- Node.js 22+ e npm 10+
- Backend Periscope rodando em `http://localhost:8080` (WildFly 34)

## Instalação

```bash
cd periscope-ui
npm ci
```

## Desenvolvimento

```bash
npm run dev
```

Abre em `http://localhost:5173/periscope/app/`. O Vite faz proxy de `/periscope/rest` → `http://localhost:8080`, então não depende de CORS (CORS também está habilitado no backend para `http://localhost:5173`).

Login padrão: `admin` / `123456`.

## Scripts

| Script | Descrição |
|--------|-----------|
| `npm run dev` | Servidor de desenvolvimento (porta 5173) |
| `npm run build` | Typecheck + build em `dist/` |
| `npm run build:war` | Build direto em `../periscope-web/src/main/webapp/app/` |
| `npm run lint` | ESLint |
| `npm run format` | Prettier (write) |
| `npm run preview` | Preview do build local |
| `npm run test:e2e` | Playwright E2E (default `http://localhost:8080/periscope/app/`) |
| `npm run test:e2e:ui` | Playwright UI mode |

## E2E (Playwright)

Requer backend WildFly + MongoDB e a SPA acessível (WAR ou `npm run dev`).

```bash
npx playwright install --with-deps chromium
npm run test:e2e
# Dev server:
E2E_BASE_URL=http://localhost:5173/periscope/app/ npm run test:e2e
```

Specs em `e2e/`: login, projects, patents, reports, harmonization.

## Variáveis de ambiente

Arquivo `.env` (ver `.env.example`):

| Variável | Default | Descrição |
|----------|---------|-----------|
| `VITE_API_BASE` | `/periscope/rest` | Base da API REST (relativa ao host) |

## Embarcar no WAR

O Maven **não** executa Node. Gere os estáticos e depois empacote o WAR:

```bash
cd periscope-ui
npm run build:war

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
cd ..
mvn -B -pl periscope-web -am package
cp periscope-web/target/periscope.war /opt/jboss/wildfly-34.0.1.Final/standalone/deployments/
```

A pasta `periscope-web/src/main/webapp/app/` está no `.gitignore`. Sem o build da SPA, o WAR sobe normalmente (JSF + REST); com o build, a SPA fica em `http://localhost:8080/periscope/app/`.

Rotas client-side (`/periscope/app/login`, etc.) são atendidas pelo `SpaFallbackFilter`, que devolve `index.html` quando o asset não existe.

Alternativa de produção: servir a pasta `dist/` atrás de um nginx com `try_files` e proxy `/periscope/rest` para o WildFly — não é necessário neste ambiente (SPA embarcada no WAR).

## Rotas da SPA

| Rota | Descrição |
|------|-----------|
| `/login` | Autenticação JWT |
| `/projects` | CRUD de projetos |
| `/projects/:id/patents` | Lista paginada de patentes |
| `/projects/:id/patents/:patentId` | Detalhe / edição |
| `/projects/:id/import` | Upload multipart |
| `/projects/:id/harmonization` | Sugestões + regras |
| `/projects/:id/reports` | Relatórios (Recharts) |
| `/users` | Usuários (somente ADMIN) |
