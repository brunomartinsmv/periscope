# Checklist de validação local

Estratégia até existir homologação: smoke manual + builds Maven + (futuro) testes automatizados.

## Pós cada fase (mínimo)

- [ ] `mvn clean package` (ou `compile`/`validate` conforme critério da fase) sem regressão inesperada
- [ ] Nenhum repositório `http://` nos POMs (a partir da Fase 1)
- [ ] Seed YAML ainda presente em `periscope-ejb/src/main/resources/`

## Smoke UI (quando houver deploy local)

1. Abrir `/periscope/` (ou context-root vigente)
2. Login com `admin` / `123456`
3. Listar projetos
4. Abrir um projeto → home
5. Navegar para importação de patentes (tela carrega)
6. Navegar para harmonização (tela carrega)
7. Abrir um relatório fixo (sem erro 500)
8. Logout → retorna ao login

## Dataset de referência

Usar conjunto pequeno de patentes (ver `docs/modernization/dataset-teste/`) para:

- Importação multi-formato (quando disponível)
- Harmonização Fast-Join
- Relatórios de agregação

## Automatização (roadmap)

| Quando | O quê |
|--------|-------|
| Fase 1 | Infra de build + JUnit 5 |
| Fase 6 | Docker Compose da stack |
| Fase 8 | CI, smoke automatizado, E2E |

## Falhas comuns

| Sintoma | Verificação |
|---------|-------------|
| Deploy `.failed` | Log WildFly `server.log` |
| Login falha | MongoDB up + SeedBean executou |
| Harmonização vazia | Índice em `PERISCOPE_DIR` + descritores seed |
