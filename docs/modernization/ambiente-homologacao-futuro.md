# Homologação futura

Não há ambiente de homologação hoje. Requisitos quando existir:

## Infraestrutura

- [ ] MongoDB dedicado com snapshot de dados **anonimizados**
- [ ] WildFly (31+) configurado via variáveis de ambiente:
  - `MONGODB_URI`
  - `PERISCOPE_DIR`
- [ ] Rede/segredos gerenciados (sem credenciais no repositório)
- [ ] Context-root e HTTPS definidos pelo ambiente

## Pipeline

- [ ] CI/CD com build Maven (JDK 21)
- [ ] Deploy automático para homologação após merge em branch de release
- [ ] Marcadores de deploy (`*.deployed` / healthcheck HTTP)
- [ ] Rollback documentado

## Qualidade

- [ ] Testes E2E contra homologação antes de produção
- [ ] Smoke checklist (login, projetos, importação, harmonização, relatório)
- [ ] Dataset de teste versionado ou restaurável a partir do snapshot

## Observabilidade

- [ ] Logs centralizados do WildFly
- [ ] Métricas básicas de disponibilidade da app e do MongoDB

## Relação com as fases

- Fase 0: documenta necessidade (este arquivo)
- Fase 6: empacota WAR + Compose (base para homologação)
- Fase 8: CI/CD e homologação operacional
