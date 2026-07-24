# Periscope

Sistema web de análise e harmonização de patentes desenvolvido na UFMT. A interface é JSF / PrimeFaces (português); a persistência é MongoDB (Morphia) e a harmonização de nomes usa Lucene.

## Stack atual

| Componente | Versão |
|------------|--------|
| Java | 21 |
| Jakarta EE | 10 |
| Servidor | WildFly 34.0.1.Final |
| UI | JSF + PrimeFaces 14 (`jakarta`) |
| Persistência | MongoDB + Morphia 2.5.3 + mongodb-driver-sync 5.2.1 |
| Harmonização | Lucene 9.12.0 |
| Empacotamento | WAR único (`periscope-web` + EJB embarcado) |

Módulos Maven: `periscope-ejb` (EJB JAR) e `periscope-web` (WAR). Artefato: `periscope-web/target/periscope.war`.

## Requisitos

**Opção A — Docker:** Docker e Docker Compose.

**Opção B — Manual:** JDK 21, Maven 3.9+, MongoDB 4.4+ (7.x recomendado) e WildFly 34.0.1.Final (JDK 21).

## Quick start (Docker Compose)

```bash
./scripts/dev-up.sh
# ou: docker compose up --build -d
```

- App: http://localhost:8080/periscope/
- Login padrão: `admin` / `123456`
- Parar: `./scripts/dev-down.sh`

Serviços: `mongodb` (`mongo:7`) e `periscope` (WildFly 34 + WAR).

## Build e deploy manual (WildFly 34)

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # ajuste se necessário

# Artefatos legados (tema bootstrap 1.0.8), se ~/.m2 estiver limpo:
./tools/install-legacy-artifacts.sh

mvn clean package
# → periscope-web/target/periscope.war

# MongoDB local (exemplo)
# mongod --dbpath /var/lib/mongodb --bind_ip 127.0.0.1 --port 27017 --fork

# Índice Lucene
mkdir -p /opt/periscope

# WildFly
cd /opt/jboss/wildfly-34.0.1.Final
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./bin/standalone.sh -b 0.0.0.0

# Em outro terminal — deploy
cp /caminho/para/periscope-web/target/periscope.war \
   /opt/jboss/wildfly-34.0.1.Final/standalone/deployments/
```

Sucesso: marcador `periscope.war.deployed` em `standalone/deployments/`. URL: http://localhost:8080/periscope/

## Variáveis de ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `MONGODB_URI` | `mongodb://localhost:27017` | URI do MongoDB |
| `MONGODB_DATABASE` | `Periscope` | Nome do banco |
| `PERISCOPE_DIR` | `/opt/periscope` | Diretório do índice Lucene (deve ser gravável) |
| `PERISCOPE_JWT_SECRET` | *(efêmero em memória)* | Segredo HS256 da API REST; defina em produção |
| `PERISCOPE_JWT_EXPIRATION_HOURS` | `8` | Validade do JWT |
| `PERISCOPE_CORS_ORIGINS` | `http://localhost:5173` | Origins CORS permitidas (separadas por vírgula) |

No Docker Compose, `MONGODB_URI` aponta para o serviço `mongodb`.

## API REST (Fase 7)

Base: `http://localhost:8080/periscope/rest`

- Health: `GET /health`
- Login: `POST /auth/login` com `{"username":"admin","password":"123456"}` → `{token,user}`
- Demais endpoints exigem `Authorization: Bearer <token>`
- Documentação completa: [docs/modernization/fase-07-spa.md](docs/modernization/fase-07-spa.md)

Exemplo:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/periscope/rest/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' | jq -r .token)
curl -s http://localhost:8080/periscope/rest/auth/me -H "Authorization: Bearer $TOKEN"
```

## Context root

`/periscope` (definido em `WEB-INF/jboss-web.xml`).

## Documentação de modernização

Ver [docs/modernization/README.md](docs/modernization/README.md).
