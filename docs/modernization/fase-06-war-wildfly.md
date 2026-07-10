# Fase 6 — WAR Único, WildFly e Docker

**PR:** `cursor/fase-6-war-wildfly-8905`  
**Depende de:** Fases 1–5  
**Bloqueia:** Fase 7

## Objetivo

Simplificar empacotamento para **WAR único**, configurar deploy em **WildFly 31+** com **Java 21**, e fornecer **Docker Compose** para ambiente local (substituto de homologação até existir ambiente dedicado).

---

## Decisões aplicadas

- Eliminar módulo `periscope-ear`
- EJB embarcado no WAR (`WEB-INF/lib/periscope-ejb.jar`)
- WildFly como servidor de aplicação
- Configuração via variáveis de ambiente

---

## Tarefas

### 6.1 Eliminar módulo EAR

**Remover:**

```
periscope-ear/          ← diretório inteiro
pom.xml                 ← remover <module>periscope-ear</module>
```

**Estrutura final:**

```
periscope/
├── pom.xml
├── periscope-ejb/      ← JAR (EJB)
└── periscope-web/      ← WAR (UI + EJB embarcado)
```

### 6.2 Configurar WAR único

**`periscope-web/pom.xml`:**

```xml
<dependency>
    <groupId>br.ufmt</groupId>
    <artifactId>periscope-ejb</artifactId>
    <version>${project.version}</version>
    <!-- scope compile (default) — embarca no WAR -->
</dependency>

<build>
    <finalName>periscope</finalName>
    <plugins>
        <plugin>
            <artifactId>maven-war-plugin</artifactId>
            <version>3.4.0</version>
            <configuration>
                <failOnMissingWebXml>false</failOnMissingWebXml>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Resultado:** `periscope-web/target/periscope.war`

### 6.3 Configuração WildFly

**Criar:** `periscope-web/src/main/webapp/WEB-INF/jboss-deployment-structure.xml`

```xml
<jboss-deployment-structure>
    <deployment>
        <exclusions>
            <!-- Excluir módulos WildFly que conflitam com libs embarcadas -->
        </exclusions>
        <dependencies>
            <!-- Declarar dependências de módulos WildFly se necessário -->
        </dependencies>
    </deployment>
</jboss-deployment-structure.xml>
```

**Validar conflitos de classpath:**
- Lucene (embarcado no WAR vs módulo WildFly)
- MongoDB driver
- Morphia

**Regra:** preferir libs embarcadas no WAR para controle de versão.

### 6.4 Variáveis de ambiente

| Variável | Obrigatória | Default | Descrição |
|----------|-------------|---------|-----------|
| `MONGODB_URI` | Não | `mongodb://mongodb:27017` | URI MongoDB |
| `MONGODB_DATABASE` | Não | `Periscope` | Nome do banco |
| `PERISCOPE_DIR` | Não | `/opt/periscope` | Diretório índice Lucene |

**WildFly — configurar via CLI ou env no Docker:**

```bash
/system-property=MONGODB_URI:add(value="mongodb://mongodb:27017")
```

### 6.5 Dockerfile — aplicação

**Criar:** `Dockerfile`

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml periscope-ejb/pom.xml periscope-web/pom.xml ./
COPY periscope-ejb ./periscope-ejb
COPY periscope-web ./periscope-web
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM quay.io/wildfly/wildfly:31.0.1.Final-jdk21
ENV MONGODB_URI=mongodb://mongodb:27017
ENV PERISCOPE_DIR=/opt/periscope
RUN mkdir -p /opt/periscope
COPY --from=build /app/periscope-web/target/periscope.war /opt/jboss/wildfly/standalone/deployments/
EXPOSE 8080
```

### 6.6 Docker Compose

**Criar:** `docker-compose.yml`

```yaml
services:
  mongodb:
    image: mongo:7
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db

  periscope:
    build: .
    ports:
      - "8080:8080"
    environment:
      MONGODB_URI: mongodb://mongodb:27017
      PERISCOPE_DIR: /opt/periscope
    depends_on:
      - mongodb
    volumes:
      - periscope_data:/opt/periscope

volumes:
  mongodb_data:
  periscope_data:
```

### 6.7 Scripts de conveniência

**Criar:** `scripts/dev-up.sh`, `scripts/dev-down.sh`

```bash
#!/bin/bash
# dev-up.sh
docker compose up --build -d
echo "Periscope: http://localhost:8080/periscope/"
```

### 6.8 Atualizar README

Substituir instruções JBoss AS 7 por:

1. Pré-requisitos: Docker, Docker Compose (ou JDK 21 + Maven + WildFly + MongoDB)
2. Quick start: `docker compose up --build`
3. Deploy manual: `mvn package && cp target/periscope.war $WILDFLY/deployments/`

### 6.9 Context root

Manter `/periscope` como context root:

- `jboss-web.xml`: `<context-root>/periscope</context-root>`
- URL: `http://localhost:8080/periscope/`

---

## Arquivos a criar/alterar

| Arquivo | Ação |
|---------|------|
| `periscope-ear/` | **Remover** |
| `pom.xml` (parent) | Remover módulo EAR |
| `periscope-web/pom.xml` | WAR finalName, dependência EJB |
| `Dockerfile` | Criar |
| `docker-compose.yml` | Criar |
| `.dockerignore` | Criar |
| `scripts/dev-up.sh` | Criar |
| `jboss-deployment-structure.xml` | Criar |
| `README.md` | Reescrever |

## Critérios de aceite

- [ ] `mvn clean package` gera `periscope.war` (sem EAR)
- [ ] `docker compose up --build` sobe MongoDB + WildFly + app
- [ ] App acessível em `http://localhost:8080/periscope/`
- [ ] Login funciona
- [ ] Importação e harmonização funcionam end-to-end
- [ ] Variáveis de ambiente configuram MongoDB e diretório Lucene
- [ ] README atualizado

## Riscos

| Risco | Mitigação |
|-------|-----------|
| Conflito de libs WildFly vs WAR | `jboss-deployment-structure.xml` |
| Permissão em `/opt/periscope` | Volume Docker dedicado |
| Startup lento (seed + reindex) | Health check com retry |

## Homologação futura

Quando ambiente existir, reutilizar mesmo `Dockerfile` e `docker-compose.yml` como base, adicionando:

- Reverse proxy (nginx/traefik)
- Secrets management para `MONGODB_URI`
- Volume persistente para MongoDB e Lucene
- Tag de imagem por versão/release
