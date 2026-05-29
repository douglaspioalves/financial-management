# Checkpoint Frontend — 2026-05-29 19:56

**Sprint:** 05 | **Story:** S-05-03-FE — Tela de Orçamento
**Agente:** Frontend
**Branch:** feature/s05-frontend-budget

---

## Feito nesta sessão

### 1. Modelo de dados
- `frontend/src/app/budget/models/budget.model.ts`
  - Interfaces `BudgetResponse` e `BudgetRequest`
  - Tipo `BudgetStatus = 'OK' | 'WARNING' | 'EXCEEDED'`

### 2. Serviço HTTP
- `frontend/src/app/budget/services/budget.service.ts`
  - `getByMonth(month: string)` → `GET /api/budgets?month=yyyy-MM-dd`
  - `create(req)` → `POST /api/budgets`
  - `update(id, req)` → `PUT /api/budgets/{id}` (inclui version)
  - `delete(id)` → `DELETE /api/budgets/{id}`

### 3. BudgetFormComponent (dialog)
- `frontend/src/app/budget/budget-form/budget-form.component.ts|html|scss`
  - Select de categoria filtrado por EXPENSE/BOTH
  - Campo limite com validação (> 0)
  - Modo criar/editar (categoria bloqueada na edição)
  - Mês pré-preenchido e exibido em pt-br, não editável
  - Tratamento de erros HTTP com snackbar
  - Loading spinner no carregamento de categorias e durante salvamento

### 4. BudgetListComponent (lista)
- `frontend/src/app/budget/budget-list/budget-list.component.ts|html|scss`
  - Navegação mensal (anterior/próximo)
  - Card de resumo (total gasto / limite total / percentual geral)
  - Lista de orçamentos por categoria com:
    - Ponto colorido da categoria + nome
    - Barra de progresso colorida por status (verde, amarelo, coral)
    - Valores em Fraunces: gasto/limite + percentual
    - Rodapé: "Restam R$ X" ou "Excedido em R$ X"
    - Botões editar e excluir
  - Skeleton loading animado (3 itens)
  - Empty state com orientação ao usuário
  - FAB fixo "+ Novo Orçamento"
  - Responsivo: 1/2/3 colunas (mobile/tablet/desktop)
  - Modo escuro via CSS variables

### 5. Roteamento
- `frontend/src/app/budget/budget.routes.ts` — rota lazy para BudgetListComponent
- `frontend/src/app/app.routes.ts` — path '/budget' com authGuard e lazy load

### Build
```
npm run build → Application bundle generation complete.
Warnings: bundle initial 511kB (pré-existente — Angular Material)
Chunk budget-list-component: 36.12 kB gerado com sucesso
Sem erros TypeScript
```

### Commits gerados
1. `feat(budget): cria modelos BudgetResponse/BudgetRequest e BudgetService HTTP`
2. `feat(budget): cria BudgetFormComponent — dialog para criar/editar orçamento`
3. `feat(budget): cria BudgetListComponent — tela de orçamentos por categoria`
4. `feat(budget): cria budget.routes.ts e adiciona rota /budget no app.routes.ts`

---

## Pendente

- PR aberto aguardando revisão do agente revisor
- Atalho para `/budget` no dashboard (pode ser feito quando o feature/s05-frontend-dashboard for mergeado — não há conflito)
- Validação end-to-end com backend rodando (endpoint `/api/budgets` em feature/s05-backend-budget)

---

## Próximo passo imediato

Merge da branch feature/s05-frontend-budget para master após revisão.
Depois: integrar atalho de orçamento no DashboardComponent novo (Sprint 05 dashboard).
