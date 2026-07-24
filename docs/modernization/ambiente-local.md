# Ambiente local mínimo

Checklist de pré-requisitos para desenvolver e validar o Periscope sem homologação.

## Pré-requisitos

| Serviço | Versão alvo | Uso | Status ambiente Cursor |
|---------|-------------|-----|------------------------|
| JDK | **21** LTS | Compilação e runtime (a partir da Fase 1) | Usar JDK 21 do sistema |
| Maven | 3.9+ | Build multi-módulo | Instalado |
| MongoDB | 7.x (hoje: 4.4 no snapshot legado) | Persistência | `mongod` local / Docker |
| WildFly | 31+ (hoje: 8.2.1 no snapshot legado) | Servidor Jakarta EE 10 | Instalar na Fase 6 |
| Docker Compose | — | Stack completa (Fase 6) | Opcional até Fase 6 |

## Diretórios e variáveis

| Variável / path | Default | Uso |
|-----------------|---------|-----|
| `PERISCOPE_DIR` | `/opt/periscope` | Índice Lucene e dados locais |
| MongoDB | `localhost:27017`, DB `Periscope` | Sem auth no baseline |
| App URL (legado) | `http://localhost:8080/periscope/` | Login `admin` / `123456` |

## Setup rápido (baseline atual / pós-Fase 0)

1. Confirmar JDK 21: `java -version`
2. Instalar artefatos legados se necessário: `tools/install-legacy-artifacts.sh`
3. Subir MongoDB (versão compatível com o driver em uso na branch)
4. Garantir `/opt/periscope` gravável
5. `mvn clean package` (critério evolui por fase)
6. Deploy no servidor da fase correspondente

## Seed YAML

Preservar e validar após Fase 1 (Jackson):

- `periscope-ejb/src/main/resources/user-inicial.yaml`
- `periscope-ejb/src/main/resources/country-inicial-data.yaml`
- `periscope-ejb/src/main/resources/applicantType-inicial.yaml`
- `periscope-ejb/src/main/resources/descriptors.yaml`

## Convenções de branch (implementação)

```
master                              → estável
cursor/fase-N-*-8905                → PRs de fase (docs + implementação)
bruno/<descricao>-2b10              → branches de agente quando novas
```

Commits: `feat(fase-N): ...` ou `chore(fase-N): ...`.
