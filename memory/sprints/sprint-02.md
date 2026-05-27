# Sprint 02
**Período:** Semanas 3–4
**Epic:** Lançamentos e Categorias
**Fatia:** 2
**Objetivo:** Registrar e listar despesas e receitas com categorias personalizáveis.
**Status:** 🟡 NÃO INICIADO

---

## Tarefas paralelas — início do sprint

```
DIA 1–3 (paralelo)
├── Backend:  S-02-01 — API de categorias
└── DBA:      migration de ajustes pós-sprint-01 (se houver)

DIA 2–6 (após categorias)
├── Backend:  S-02-02 — API de lançamentos (CRUD)
└── Frontend: S-02-01-FE começa módulo de categorias

DIA 5–9 (após API lançamentos)
├── Frontend: S-02-03 — Tela de lançamentos
└── QA:       Testes de TransactionService e endpoints

DIA 9–10 (fechamento)
├── Revisor:  Revisão de segurança e qualidade
└── DevOps:   Garantir docker compose sobe limpo + commit
```

---

## Backlog do sprint

### S-02-01 · API de categorias
**Papel:** Backend + DBA | **Pontos:** 2 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade Category + CategoryRepository | Backend | PENDENTE |
| CategoryService (CRUD) | Backend | PENDENTE |
| CategoryController (GET, POST, PUT, DELETE) | Backend | PENDENTE |
| Decidir comportamento de exclusão com lançamentos existentes | Arquiteto | PENDENTE |
| Registrar decisão em memory/decisions/ | Arquiteto | PENDENTE |
| Testes unitários de CategoryService | QA | PENDENTE |

---

### S-02-02 · API de lançamentos (CRUD)
**Papel:** Backend | **Pontos:** 8 | **Depende de:** S-02-01 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade Transaction + TransactionRepository | Backend | PENDENTE |
| TransactionService (CRUD + filtro por mês) | Backend | PENDENTE |
| TransactionController (GET, POST, PUT, DELETE) | Backend | PENDENTE |
| Validações (valor positivo, data, categoria, paid_by) | Backend | PENDENTE |
| Lançamento à vista não gera Installment | Backend | PENDENTE |
| Testes de integração dos endpoints | QA | PENDENTE |

---

### S-02-03 · Tela de lançamentos
**Papel:** Frontend | **Pontos:** 6 | **Depende de:** S-02-02 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo transactions | Frontend | PENDENTE |
| Componente de lista com filtro de mês | Frontend | PENDENTE |
| Formulário reativo de novo/editar lançamento | Frontend | PENDENTE |
| Serviço HTTP TransactionService | Frontend | PENDENTE |
| Seleção de categoria no formulário | Frontend | PENDENTE |
| Componente de confirmação de exclusão | Frontend | PENDENTE |
| Exibição de valor +/− com cores do design system | Frontend | PENDENTE |

---

## Progresso diário

| Dia | Data | Stories avançadas | Impedimentos |
|---|---|---|---|
| 1 | — | — | — |
| 2 | — | — | — |
| 3 | — | — | — |
| 4 | — | — | — |
| 5 | — | — | — |
| 6 | — | — | — |
| 7 | — | — | — |
| 8 | — | — | — |
| 9 | — | — | — |
| 10 | — | — | — |

---

## Definition of Done

- [ ] S-02-01: CRUD de categorias funcionando; seeds visíveis
- [ ] S-02-02: CRUD de lançamentos; filtro por mês correto
- [ ] S-02-03: Tela de lançamentos operacional no browser
- [ ] Decisão de exclusão de categoria registrada em memory/decisions/
- [ ] Testes passando: `./mvnw test`
- [ ] Review registrada em `memory/reviews/review-sprint-02.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-02.md`
- [ ] Learnings atualizados em `memory/learnings/`
