# Sprint 05
**Período:** Semanas 9–10
**Epic:** Dashboard + Orçamento
**Fatias:** 4 e 5a
**Objetivo:** Painel financeiro do mês com gráficos e controle de orçamento por categoria.
**Status:** 🟡 NÃO INICIADO

---

## Tarefas paralelas — início do sprint

```
DIA 1–5 (paralelo)
├── Backend:  S-05-01 — API de dashboard (agregações)
└── Backend:  S-05-03 — API de orçamento
   (podem ser desenvolvidas em paralelo por serem independentes)

DIA 3–7 (paralelo ao backend)
└── QA:       Preparar testes de agregação (mês com parcelas, sem receita)

DIA 5–9 (após APIs)
├── Frontend: S-05-02 — Tela de dashboard
└── Frontend: S-05-03-FE — Tela de orçamento

DIA 9–10 (fechamento)
├── Revisor:  Revisão geral
└── DevOps:   Commit + validação Docker
```

---

## Backlog do sprint

### S-05-01 · API de dashboard
**Papel:** Backend | **Pontos:** 5 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| DashboardService com agregações por mês | Backend | PENDENTE |
| Agregação de receitas e despesas (usando Installment para parcelados) | Backend | PENDENTE |
| Gastos por categoria com percentual | Backend | PENDENTE |
| Comparativo com mês anterior | Backend | PENDENTE |
| Últimos 10 lançamentos do mês | Backend | PENDENTE |
| Endpoint GET /api/dashboard?month=yyyy-MM | Backend | PENDENTE |
| Testes de agregação (mês com parcelas, mês sem dados) | QA | PENDENTE |

---

### S-05-02 · Tela de dashboard
**Papel:** Frontend | **Pontos:** 6 | **Depende de:** S-05-01 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo dashboard como tela inicial | Frontend | PENDENTE |
| Cards de receita, despesa e saldo (cores design system) | Frontend | PENDENTE |
| Integrar biblioteca de gráficos (donut por categoria) | Frontend | PENDENTE |
| Lista dos últimos lançamentos | Frontend | PENDENTE |
| Comparativo mês anterior (variação em %) | Frontend | PENDENTE |
| Navegação por mês (anterior / próximo) | Frontend | PENDENTE |
| Responsividade em mobile | Frontend | PENDENTE |

---

### S-05-03 · API de orçamento
**Papel:** Backend | **Pontos:** 4 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade Budget + BudgetRepository | Backend | PENDENTE |
| BudgetService (definir limite + calcular gasto real vs. limite) | Backend | PENDENTE |
| BudgetController (GET e POST /api/budgets) | Backend | PENDENTE |
| Percentual consumido por categoria | Backend | PENDENTE |
| Testes de estouro de orçamento | QA | PENDENTE |

---

### S-05-03-FE · Tela de orçamento
**Papel:** Frontend | **Pontos:** 3 | **Depende de:** S-05-03 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo budget com barras de progresso | Frontend | PENDENTE |
| Cores progressivas (verde → amarelo → vermelho) | Frontend | PENDENTE |
| Alerta visual ao ultrapassar limite | Frontend | PENDENTE |
| Formulário para definir limite por categoria | Frontend | PENDENTE |

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

- [ ] S-05-01: Dashboard retorna dados corretos com parcelas no mês certo
- [ ] S-05-02: Painel funcional com gráfico e comparativo
- [ ] S-05-03: Orçamento por categoria com % consumido correto
- [ ] S-05-03-FE: Barras de progresso e alertas funcionando
- [ ] Testes passando: `./mvnw test`
- [ ] Review registrada em `memory/reviews/review-sprint-05.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-05.md`
- [ ] Learnings atualizados em `memory/learnings/`
