# Review — Sprint 01
**Data:** 2026-05-28
**Sprint:** 01 de 07 | **Fatias:** 0 (Esqueleto) e 1 (Autenticação)
**Conduzido por:** Scrum Master
**Participantes:** DevOps, DBA, Backend, Frontend, QA, Revisor

---

## Objetivo do sprint — foi atingido?

**Sim** — app rodando de ponta a ponta com login funcional, testes passando e Docker Compose configurado.

---

## O que foi entregue ✅

| Story | Descrição | Agente | Evidência |
|---|---|---|---|
| S-01-01 | Esqueleto monorepo + Docker Compose | DevOps | `docker-compose.yml`, Dockerfiles, `mvnw package` e `ng build` OK |
| S-01-02 | Migrations iniciais (schema + seeds) | DBA | V1–V3 aplicadas; V5 seeds de usuários de teste adicionada no fechamento |
| S-01-03 | Cadastro e login com JWT | Backend | 15 testes passando (`./mvnw test` BUILD SUCCESS) |
| S-01-04 | Telas de login e cadastro Angular | Frontend | `npm run build` BUILD SUCCESS; telas funcionais com design system |
| CI | GitHub Actions para backend e frontend | DevOps | `.github/workflows/ci.yml` operacional |
| QA auth | Testes de integração register/login | QA | 15 testes, 0 falhas; bugs de 403/401 corrigidos |
| Revisão segurança | JWT, BCrypt, exposição de dados | Revisor | `@JsonIgnore` em senha, `AuthenticationEntryPoint` configurado |

## O que NÃO foi entregue ❌

| Story | Motivo | Vai para o próximo sprint? |
|---|---|---|
| Validação `docker compose up` com banco zerado | Docker Desktop offline no momento da validação | Validar no início do Sprint 02 |

## Demonstração (o que está funcionando)

Fluxo validado via testes e build:
- `POST /api/auth/register` → cria conta, retorna JWT
- `POST /api/auth/login` → autentica, retorna JWT
- Endpoints protegidos retornam 401 sem token, 404/200 com token válido
- Frontend: formulários de login e cadastro com validações em pt-br, toggle claro/escuro, guards de rota

Usuários de teste disponíveis após `docker compose up --build`:
- `alice@example.com` / `senha123`
- `bob@example.com` / `senha123`

---

## Métricas do sprint

- Stories planejadas: 4 (S-01-01 a S-01-04)
- Stories entregues: 4
- Stories carregadas para o próximo sprint: 0
- Testes passando: ✅ (15/15)
- Docker Compose sobe limpo: ⚠️ (build validado; execução com banco zerado pendente)

## Feedback e observações

- Sprint 01 acelerado — todas as 4 stories entregues em 2 dias úteis (Dias 1–2).
- O volume de trabalho de Sprint 02 já foi incorporado ao master durante o Sprint 01 (avançamento espontâneo).
- JWT_SECRET não-Base64 causou falha na inicialização; corrigido com fallback UTF-8 no `JwtService`.
- Tabela `users` (não `user`) — armadilha do PostgreSQL descoberta e documentada nos learnings.

---

## Próximos passos

- [x] Realizar retrospectiva (`memory/retros/retro-sprint-01.md`)
- [x] Learnings atualizados em `memory/learnings/`
- [ ] Validar `docker compose up --build` com banco zerado no início do Sprint 02
- [ ] Iniciar Sprint 02 Planning
