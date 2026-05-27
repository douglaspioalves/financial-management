# Sprint 01
**Período:** Semanas 1–2
**Epic:** Fundação e Autenticação
**Fatias:** 0 e 1
**Objetivo:** Ter o app rodando de ponta a ponta com login funcional.
**Status:** 🟡 EM ANDAMENTO
**Período:** 2026-05-26 → 2026-06-06

---

## Tarefas paralelas — início do sprint

```
DIA 1–3 (paralelo)
├── DevOps:  S-01-01 — Esqueleto + Docker Compose
└── DBA:     aguarda S-01-01 para migrations

DIA 2–4 (após esqueleto)
├── DBA:     S-01-02 — Migrations iniciais
└── DevOps:  README + .env.example

DIA 3–7 (após migrations)
├── Backend: S-01-03 — Cadastro/login JWT
└── Frontend: S-01-04 começa tema + estrutura Angular Material

DIA 5–10 (após backend login)
└── Frontend: S-01-04 — Telas de login/cadastro completas

DIA 9–10 (fechamento)
├── QA:      Testes de autenticação
└── Revisor: Revisão de segurança (JWT, BCrypt, endpoints)
```

---

## Backlog do sprint

### S-01-01 · Esqueleto do monorepo e Docker Compose
**Papel:** DevOps | **Pontos:** 3 | **Status:** `CONCLUÍDO`

| Tarefa | Papel | Status |
|---|---|---|
| Criar projeto Spring Boot (Initializr) | DevOps | CONCLUÍDO |
| Criar projeto Angular + Angular Material | DevOps | CONCLUÍDO |
| Escrever docker-compose.yml | DevOps | CONCLUÍDO |
| Criar .env.example | DevOps | CONCLUÍDO |
| Implementar GET /api/health | Backend | CONCLUÍDO |
| Componente Angular inicial chamando /api/health | Frontend | CONCLUÍDO |
| README com instruções Windows | DevOps | CONCLUÍDO |

---

### S-01-02 · Migration inicial do banco
**Papel:** DBA | **Pontos:** 2 | **Depende de:** S-01-01 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| V1__initial_schema.sql (todas as tabelas) | DBA | PENDENTE |
| Índices, FKs e constraints | DBA | PENDENTE |
| V2__seeds.sql (2 persons + 12 categorias) | DBA | PENDENTE |
| Validar migrations num banco zerado | DevOps | PENDENTE |

---

### S-01-03 · Cadastro e login com JWT
**Papel:** Backend | **Pontos:** 5 | **Depende de:** S-01-02 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade User + UserRepository | Backend | PENDENTE |
| AuthService (register + login + JWT) | Backend | PENDENTE |
| AuthController (/api/auth/register e /login) | Backend | PENDENTE |
| Spring Security (filtro JWT, rotas) | Backend | PENDENTE |
| Testes de integração register/login | QA | PENDENTE |

---

### S-01-04 · Telas de login e cadastro
**Papel:** Frontend | **Pontos:** 4 | **Depende de:** S-01-03 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Tema Angular Material (paleta design system) | Frontend | PENDENTE |
| Módulo auth + telas login/cadastro | Frontend | PENDENTE |
| AuthService frontend (HTTP + token) | Frontend | PENDENTE |
| AuthGuard | Frontend | PENDENTE |
| Toggle claro/escuro | Frontend | PENDENTE |

---

## Progresso diário

| Dia | Data | Stories avançadas | Impedimentos |
|---|---|---|---|
| 1 | 2026-05-26 (ter) | S-01-01 CONCLUÍDA — backend + frontend + Docker Compose + README | — |
| 2 | 2026-05-27 (qua) | — | — |
| 3 | 2026-05-28 (qui) | — | — |
| 4 | 2026-05-29 (sex) | — | — |
| 5 | 2026-06-01 (seg) | — | — |
| 6 | 2026-06-02 (ter) | — | — |
| 7 | 2026-06-03 (qua) | — | — |
| 8 | 2026-06-04 (qui) | — | — |
| 9 | 2026-06-05 (sex) | — | — |
| 10 | 2026-06-06 (seg) | — | — |

---

## Definition of Done

- [x] S-01-01: `docker compose up --build` sobe tudo sem erro
- [ ] S-01-02: Migrations aplicadas; seeds presentes no banco
- [ ] S-01-03: Registro/login funcionando; 401 em rotas protegidas
- [ ] S-01-04: Login no browser; modo claro/escuro funcionando
- [ ] Testes passando: `./mvnw test`
- [ ] Review registrada em `memory/reviews/review-sprint-01.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-01.md`
- [ ] Learnings atualizados em `memory/learnings/`
