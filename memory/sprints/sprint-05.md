# Sprint 05
**Período:** Semanas 9–10
**Epic:** Dashboard + Orçamento
**Fatias:** 4 e 5a
**Objetivo:** Painel financeiro do mês com gráficos e controle de orçamento por categoria.
**Status:** ✅ CONCLUÍDO

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
**Papel:** Backend | **Pontos:** 5 | **Status:** `CONCLUÍDO`

| Tarefa | Papel | Status |
|---|---|---|
| DashboardService com agregações por mês | Backend | CONCLUÍDO |
| Agregação de receitas e despesas (usando Installment para parcelados) | Backend | CONCLUÍDO |
| Gastos por categoria com percentual | Backend | CONCLUÍDO |
| Comparativo com mês anterior | Backend | CONCLUÍDO |
| Últimos 10 lançamentos do mês | Backend | CONCLUÍDO |
| Endpoint GET /api/dashboard?month=yyyy-MM | Backend | CONCLUÍDO |
| Testes de agregação (mês com parcelas, mês sem dados) | QA | CONCLUÍDO |

---

### S-05-02 · Tela de dashboard
**Papel:** Frontend | **Pontos:** 6 | **Depende de:** S-05-01 | **Status:** `CONCLUÍDO`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo dashboard como tela inicial | Frontend | CONCLUÍDO |
| Cards de receita, despesa e saldo (cores design system) | Frontend | CONCLUÍDO |
| Integrar biblioteca de gráficos (donut por categoria) | Frontend | CONCLUÍDO |
| Lista dos últimos lançamentos | Frontend | CONCLUÍDO |
| Comparativo mês anterior (variação em %) | Frontend | CONCLUÍDO |
| Navegação por mês (anterior / próximo) | Frontend | CONCLUÍDO |
| Responsividade em mobile | Frontend | CONCLUÍDO |

---

### S-05-03 · API de orçamento
**Papel:** Backend | **Pontos:** 4 | **Status:** `CONCLUÍDO`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade Budget + BudgetRepository | Backend | CONCLUÍDO |
| BudgetService (definir limite + calcular gasto real vs. limite) | Backend | CONCLUÍDO |
| BudgetController (GET e POST /api/budgets) | Backend | CONCLUÍDO |
| Percentual consumido por categoria | Backend | CONCLUÍDO |
| Testes de estouro de orçamento | QA | CONCLUÍDO |

---

### S-05-03-FE · Tela de orçamento
**Papel:** Frontend | **Pontos:** 3 | **Depende de:** S-05-03 | **Status:** `CONCLUÍDO`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo budget com barras de progresso | Frontend | CONCLUÍDO |
| Cores progressivas (verde → amarelo → vermelho) | Frontend | CONCLUÍDO |
| Alerta visual ao ultrapassar limite | Frontend | CONCLUÍDO |
| Formulário para definir limite por categoria | Frontend | CONCLUÍDO |

---

## Progresso diário

| Dia | Data | Stories avançadas | Impedimentos |
|---|---|---|---|
| 1 | 2026-05-29 | S-05-01, S-05-03 | — |
| 2 | 2026-05-29 | S-05-02, S-05-03-FE, revisão, testes | — |
| 3–10 | — | — | — |

---

## Definition of Done

- [x] S-05-01: Dashboard retorna dados corretos com parcelas no mês certo
- [x] S-05-02: Painel funcional com gráfico e comparativo
- [x] S-05-03: Orçamento por categoria com % consumido correto
- [x] S-05-03-FE: Barras de progresso e alertas funcionando
- [x] Testes passando: `mvn test` → BUILD SUCCESS (102 testes, 0 falhas, 17 skipped)
- [x] Review registrada em `memory/reviews/review-sprint-05.md`
- [x] Tag sprint-05 criada no commit de merge no master

## Merges realizados (2026-05-29)

| Branch | Commit de merge | Status |
|---|---|---|
| docs/s05-planning | 0f5971e | ✅ |
| docs/s05-review | bebb5a0 | ✅ |
| feature/s05-schema | 294bb74 | ✅ |
| feature/s05-backend-dashboard | 91a9cf1 | ✅ |
| feature/s05-backend-budget | b72b821 (resolvido conflito) | ✅ |
| fix/budget-remaining-amount | a0969dc | ✅ |
| feature/s05-tests | e6203ce (resolvido conflito) | ✅ |
| fix/sprint05-tests | 83cd4f6 | ✅ |
| feature/s05-frontend-dashboard | 2f843c6 | ✅ |
| feature/s05-frontend-budget | 7999a84 (resolvido conflito R-01) | ✅ |

## Conflitos resolvidos

- **InstallmentRepository.java** — combinados métodos do dashboard (`findExpenseInstallmentsByMonth`) e orçamento (`sumByCategoryAndReferenceMonth`).
- **TransactionRepository.java** — combinados métodos do dashboard (`findCashExpensesByMonth`, `findIncomesByMonth`, `findRecentByMonth`) e orçamento (`sumNonInstallmentByTypeAndCategoryAndDateBetween`).
- **BudgetServiceTest.java** — mantida versão completa do HEAD (com mocks reais) sobre a versão @Disabled do branch de testes.
- **app.routes.ts** (R-01) — mantidas todas as rotas de ambas as branches; wildcard redireciona para `/dashboard`.
