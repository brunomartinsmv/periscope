# Fase 8 — Qualidade, CI/CD e Homologação Futura

**PR:** `cursor/fase-8-qualidade-ci-8905`  
**Depende de:** Fase 1 (mínimo); expande continuamente  
**Paralelo com:** Fases 6–7

## Objetivo

Estabelecer **pipeline de qualidade**, **testes automatizados** e **preparação para ambiente de homologação** que ainda não existe hoje, mas será necessário no futuro.

---

## Contexto

Sem homologação hoje, a validação depende de:
1. Testes automatizados locais (CI)
2. Docker Compose como ambiente reproduzível
3. Checklists manuais (Fase 0)

Esta fase constrói a ponte para quando homologação existir.

---

## Tarefas

### 8.1 CI básico — GitHub Actions

**Criar:** `.github/workflows/ci.yml`

```yaml
name: CI
on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - run: mvn clean verify -DskipTests
      - run: mvn test
        if: false  # habilitar quando testes existirem

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
      - run: cd periscope-ui && npm ci && npm run build
        if: false  # habilitar após Fase 7
```

**Evolução:** habilitar testes progressivamente conforme fases avancem.

### 8.2 Testes unitários — backend

**Framework:** JUnit 5 + Mockito + AssertJ

| Área | Classe de teste | Prioridade |
|------|-----------------|------------|
| Fast-Join / Lucene | `FastJoinRegressionTest` | P0 |
| YamlLoader / Seed | `YamlLoaderTest` | P0 |
| Repositórios MongoDB | `PatentRepositoryTest` | P1 |
| Importadores | `ESPACENETPatentImporterTest` | P1 |
| DTOs / Services REST | `ProjectResourceTest` | P2 |
| PDFBox | `PDFTextParserTest` | P2 |

**Localização:** `periscope-ejb/src/test/java/`, `periscope-web/src/test/java/`

### 8.3 Testes de integração — Testcontainers

**Dependência:**

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <version>1.20.0</version>
    <scope>test</scope>
</dependency>
```

**Exemplo:**

```java
@Testcontainers
class PatentRepositoryIT {
    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Test
    void shouldSaveAndFindPatent() { ... }
}
```

**Requer:** Docker no CI (GitHub Actions suporta nativamente).

### 8.4 Testes E2E — backend (existente)

**Arquivo existente:** `periscope-web/src/test/resources/br/ufmt/periscope/features/login.feature`

**Atualizar:** Cucumber + Selenium/Playwright para Jakarta EE.

**Alternativa moderna:** Playwright ou Cypress contra SPA (Fase 7).

### 8.5 Testes E2E — frontend (pós-Fase 7)

**Criar:** `periscope-ui/e2e/`

```typescript
// login.spec.ts
test('login with valid credentials', async ({ page }) => {
  await page.goto('http://localhost:3000/login');
  await page.fill('[name=username]', 'admin');
  await page.fill('[name=password]', '123456');
  await page.click('button[type=submit]');
  await expect(page).toHaveURL(/projects/);
});
```

### 8.6 Análise de dependências (CVE)

**Adicionar ao CI:**

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>10.0.4</version>
</plugin>
```

```bash
mvn org.owasp:dependency-check-maven:check
```

### 8.7 Cobertura de código

**Plugin JaCoCo:**

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

**Meta inicial:** 40% backend; 60% após Fase 7.

### 8.8 Linting e formatação

| Ferramenta | Escopo |
|------------|--------|
| Spotless (Java) | Formatação consistente |
| ESLint + Prettier | Frontend SPA |
| EditorConfig | `.editorconfig` na raiz |

### 8.9 Preparação homologação futura

**Criar:** `docs/modernization/ambiente-homologacao-futuro.md`

#### Infraestrutura mínima

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   nginx     │───►│  WildFly    │───►│  MongoDB    │
│  (SSL/TLS)  │    │  (periscope)│    │  (dados)    │
└─────────────┘    └─────────────┘    └─────────────┘
       │
       ▼
┌─────────────┐
│  SPA (UI)   │
└─────────────┘
```

#### Pipeline de deploy (futuro)

```yaml
# .github/workflows/deploy-staging.yml (NÃO implementar agora)
name: Deploy Staging
on:
  push:
    tags: ['v*']
jobs:
  deploy:
    environment: staging
    steps:
      - build Docker images
      - push to registry
      - deploy to staging server
      - run E2E tests against staging URL
      - notify team
```

#### Checklist pré-homologação

- [ ] Servidor com Docker + Docker Compose
- [ ] MongoDB com volume persistente
- [ ] DNS: `periscope-hom.ufmt.br` (exemplo)
- [ ] Certificado SSL
- [ ] Snapshot de dados anonimizados
- [ ] Secrets: `MONGODB_URI`, JWT secret
- [ ] Monitoramento básico (health endpoint)

#### Health check

**Criar:** `HealthResource.java`

```java
@Path("/api/health")
public class HealthResource {
    @GET
    public Response check() {
        // MongoDB ping + Lucene index exists
        return Response.ok(Map.of("status", "UP")).build();
    }
}
```

### 8.10 Observabilidade básica

| Componente | Ferramenta | Fase |
|------------|------------|------|
| Logging | SLF4J + JSON (WildFly) | Agora |
| Health | MicroProfile Health ou custom | Agora |
| Métricas | MicroProfile Metrics | Homologação |
| Tracing | OpenTelemetry | Futuro |

### 8.11 Documentação API

**Adicionar:** OpenAPI 3 via MicroProfile OpenAPI ou Swagger.

```java
@OpenAPIDefinition(info = @Info(title = "Periscope API", version = "2.0"))
public class JaxRsActivator extends Application { ... }
```

**Endpoint:** `/periscope/openapi` ou `/periscope/swagger-ui`

---

## Arquivos a criar

| Arquivo | Descrição |
|---------|-----------|
| `.github/workflows/ci.yml` | Pipeline CI |
| `.github/workflows/deploy-staging.yml` | Template futuro (comentado) |
| `.editorconfig` | Formatação |
| `periscope-ejb/src/test/java/...` | Testes unitários e integração |
| `periscope-ui/e2e/` | Testes E2E frontend |
| `docs/modernization/ambiente-homologacao-futuro.md` | Guia infra futura |
| `HealthResource.java` | Health check |

## Critérios de aceite

- [ ] CI executa `mvn verify` em cada push/PR
- [ ] Testes Fast-Join (Lucene) passam no CI
- [ ] Testcontainers MongoDB funciona no CI
- [ ] JaCoCo gera relatório de cobertura
- [ ] OWASP dependency-check executa (warning, não bloqueante inicialmente)
- [ ] Health endpoint responde `UP`
- [ ] Documento de homologação futura completo
- [ ] Template de pipeline de deploy documentado (não ativo)

## Cronograma sugerido (dentro da fase)

| Entrega | Quando |
|---------|--------|
| CI básico (build) | Imediato (após Fase 1) |
| Testes Lucene | Após Fase 4 |
| Testcontainers MongoDB | Após Fase 3 |
| E2E SPA | Após Fase 7 |
| Deploy homologação | Quando infra existir |

## Riscos

| Risco | Mitigação |
|-------|-----------|
| CI lento (Testcontainers) | Reutilizar container entre testes |
| Testes flaky (Lucene/Selenium) | Datasets determinísticos |
| Homologação nunca criada | Docker Compose como substituto permanente |

## Nota

Esta fase **nunca termina** — qualidade é contínua. O PR de planejamento define a base; cada fase de implementação deve adicionar testes correspondentes.
