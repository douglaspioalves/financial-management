# Sprint Planning — Sprint 05

**Data:** 2026-05-29
**Sprint:** 05 (Semanas 9–10)
**Objetivo:** Dashboard financeiro do mês + CRUD de orçamento por categoria
**Fatias cobertas:** Fatia 4 (Dashboard) + Fatia 5a (Orçamento)
**Fatia postergada:** Fatia 5b (Recorrência / RecurringRule) → Sprint 06

---

## Contexto

- Sprints 01–04 concluídos e mergeados em `master`.
- Schema: migrations V1–V7 aplicadas. Tabelas `transaction`, `installment`, `budget`,
  `category`, `person` já existem com todos os índices necessários.
- Nenhuma migration nova é necessária para este sprint.
- Contrato da API definido em `docs/api.md` (seções "Fatia 4" e "Fatia 5").

---

## Stories do Sprint 05

### S-05-01 · API de dashboard
**Agente:** Backend
**Pontos:** 5
**Branch:** `feature/s05-backend`

**Critérios de aceite:**
- `GET /api/dashboard?month=yyyy-MM` retorna: totalIncome, totalExpense, balance,
  previousMonth (mesmos campos), incomeVariation, expenseVariation,
  expenseByCategory (lista com %) e recentTransactions (últimos 10).
- Lançamentos parcelados (CREDIT + `installmentsTotal > 1`): soma pelo `installment.amount`
  cujo `reference_month` = mês consultado.
- Lançamentos à vista e receitas: soma pelo `transaction.amount` onde `transaction.date`
  cai no mês consultado.
- `incomeVariation` e `expenseVariation` retornam `null` se o mês anterior tiver
  total zero (não dividir por zero).
- `expenseByCategory` vazia se `totalExpense = 0`.
- `expenseByCategory` ordenada por `total` decrescente.
- `recentTransactions` limitada a 10 itens, ordenada por `date` decrescente + `created_at`
  decrescente.
- Endpoint protegido por JWT.

**Tarefas:**
- [ ] Criar `DashboardController` — GET `/api/dashboard` — Backend
- [ ] Criar `DashboardService` com método `calculate(YearMonth month)` — Backend
- [ ] Query JPQL/SQL para somar `installment.amount` por `reference_month` (parcelados) — Backend
- [ ] Query JPQL/SQL para somar `transaction.amount` por `transaction.date` (à vista) — Backend
- [ ] Query para `previousMonth` (reutilizar lógica, passando mês anterior) — Backend
- [ ] Calcular `incomeVariation` e `expenseVariation` com tratamento de divisão por zero — Backend
- [ ] Query agrupada por `category` para `expenseByCategory` + cálculo de `percentage` — Backend
- [ ] Query para `recentTransactions` (limit 10, date desc, created_at desc) — Backend
- [ ] Criar `DashboardResponse` DTO e sub-DTOs — Backend
- [ ] Validar parâmetro `month` (ausente → 400; formato inválido → 400) — Backend

---

### S-05-01-QA · Testes de dashboard
**Agente:** QA
**Pontos:** 4
**Branch:** `feature/s05-tests`
**Depende de:** S-05-01 (ao menos a camada service)

**Critérios de aceite:**
- Testes unitários cobrem todos os cenários críticos listados abaixo.
- Testes de integração cobrem o endpoint GET com ao menos 3 cenários.

**Cenários obrigatórios:**
1. Mês só com lançamentos à vista — totalExpense e totalIncome corretos.
2. Mês com compra parcelada em 3x — só a parcela do mês entra no total (não o valor cheio).
3. Mês sem receita — totalIncome = 0, incomeVariation = null.
4. Mês sem despesa — totalExpense = 0, expenseByCategory = [].
5. Mês anterior sem receita — incomeVariation = null (não NaN, não erro 500).
6. Mês anterior sem despesa — expenseVariation = null.
7. Variação positiva (gasto aumentou) — expenseVariation > 0.
8. Variação negativa (gasto diminuiu) — expenseVariation < 0.
9. Despesas em 3 categorias — expenseByCategory tem 3 itens, soma dos percentages ≈ 100%.
10. recentTransactions limitada a 10 mesmo com 15 lançamentos no mês.

---

### S-05-02 · Tela de dashboard
**Agente:** Frontend
**Pontos:** 6
**Branch:** `feature/s05-frontend`
**Depende de:** S-05-01

**Critérios de aceite:**
- Módulo `dashboard` criado; é a rota padrão pós-login (`/dashboard`).
- Cards de receita (verde-menta), despesa (coral) e saldo (azul) com valores em fonte display
  (Fraunces), sinal +/− visível.
- Variação em relação ao mês anterior exibida com seta para cima/baixo e percentual;
  campo não exibido se `null`.
- Gráfico de pizza/donut por categoria, usando as cores de `categoryColor`.
  Biblioteca: `Chart.js` via wrapper `ng2-charts` ou equivalente já presente no projeto.
- Lista dos últimos 10 lançamentos com data, descrição, categoria, valor e tipo.
- Navegação por mês (setas anterior/próximo + exibição "Maio 2026").
- Responsivo (mobile-first).
- Estados vazios: mês sem dados exibe mensagem "Nenhum lançamento encontrado neste mês."
- Loading skeleton enquanto a chamada está em progresso.
- Modo escuro funcional.

**Tarefas:**
- [ ] Criar módulo `dashboard` com rota `/dashboard` como padrão pós-login — Frontend
- [ ] Componente de card resumo (receita/despesa/saldo) reutilizável — Frontend
- [ ] Componente de variação percentual (seta + %) — Frontend
- [ ] Integrar gráfico de donut (Chart.js/ng2-charts) com dados de `expenseByCategory` — Frontend
- [ ] Componente de lista de transações recentes — Frontend
- [ ] Navegação por mês (serviço ou utilitário de mês) — Frontend
- [ ] Loading skeleton nas seções de cards e gráfico — Frontend
- [ ] Estado vazio para mês sem dados — Frontend
- [ ] Serviço `DashboardService` (HTTP) — Frontend

---

### S-05-03 · API de orçamento
**Agente:** Backend
**Pontos:** 4
**Branch:** `feature/s05-backend` (mesma branch do dashboard)

**Critérios de aceite:**
- `GET /api/budgets?month=yyyy-MM` lista orçamentos do mês com `spentAmount` calculado.
- `POST /api/budgets` cria orçamento; rejeita categoria inativa, tipo incompatível e duplicata.
- `PUT /api/budgets/{id}` atualiza `limitAmount` com optimistic locking; retorna 409 em conflito.
- `DELETE /api/budgets/{id}` remove orçamento; retorna 204.
- `spentAmount` usa a mesma lógica de agregação do dashboard (parcelas pelo reference_month).
- `status` calculado corretamente: OK (≤70%), WARNING (>70%–≤100%), EXCEEDED (>100%).
- `remainingAmount` = limitAmount − spentAmount (pode ser negativo).

**Tarefas:**
- [ ] Criar entidade `Budget` com `@Version` para optimistic locking — Backend
- [ ] Criar `BudgetRepository` — Backend
- [ ] Criar `BudgetService` com métodos: `findByMonth`, `create`, `update`, `delete` — Backend
- [ ] Reutilizar query de gasto real do `DashboardService` no `BudgetService` — Backend
- [ ] Criar `BudgetController` com os 4 endpoints — Backend
- [ ] Criar `BudgetRequest` e `BudgetResponse` DTOs — Backend
- [ ] Validação de unicidade (category + month) com mensagem adequada — Backend
- [ ] Validação de tipo da categoria (só EXPENSE ou BOTH) — Backend

---

### S-05-03-QA · Testes de orçamento
**Agente:** QA
**Pontos:** 3
**Branch:** `feature/s05-tests`
**Depende de:** S-05-03

**Cenários obrigatórios:**
1. Criar orçamento com `limitAmount = 1000`; gasto = 0 → status OK, percentage = 0.
2. Criar orçamento; lançar R$ 700 na categoria → status OK (70%), lançar mais R$ 1 → WARNING.
3. Ultrapassar o limite → status EXCEEDED, percentage > 100, remainingAmount negativo.
4. Tentar criar duplicata (mesma categoria + mês) → 409.
5. Tentar criar orçamento para categoria INCOME → 422.
6. Tentar criar orçamento para categoria inexistente → 404.
7. PUT com versão desatualizada → 409.
8. PUT com `limitAmount` ≤ 0 → 400.
9. DELETE de orçamento inexistente → 404.
10. `spentAmount` contabiliza parcela do mês correto (não o valor total da compra parcelada).

---

### S-05-04 · Tela de orçamento
**Agente:** Frontend
**Pontos:** 5
**Branch:** `feature/s05-frontend` (mesma branch do dashboard)
**Depende de:** S-05-03

**Critérios de aceite:**
- Módulo `budget` com rota `/budget`.
- Lista de orçamentos do mês com barras de progresso coloridas por status:
  - `OK`: verde-menta
  - `WARNING`: areia (amarelo)
  - `EXCEEDED`: coral (vermelho)
- Exibe: nome da categoria, `spentAmount / limitAmount`, percentual e `remainingAmount`.
- Alerta visual destacado quando algum orçamento está `EXCEEDED`.
- Formulário de criação: seleção de categoria (apenas EXPENSE/BOTH), mês, limite.
- Formulário de edição (apenas `limitAmount`).
- Confirmação de exclusão.
- Navegação por mês (mesmo componente do dashboard, se possível).
- Estado vazio: "Nenhum orçamento definido para este mês."
- Loading skeleton.
- Responsivo e modo escuro funcional.

**Tarefas:**
- [ ] Criar módulo `budget` com rota `/budget` — Frontend
- [ ] Componente de lista de orçamentos com barras de progresso — Frontend
- [ ] Componente de barra de progresso por status (reutilizável, shared) — Frontend
- [ ] Alerta de categoria excedida (banner ou card destacado) — Frontend
- [ ] Formulário reativo de criação de orçamento — Frontend
- [ ] Formulário reativo de edição (limitAmount + versão) — Frontend
- [ ] Confirmação de exclusão — Frontend
- [ ] Serviço `BudgetService` (HTTP) — Frontend
- [ ] Estado vazio e loading skeleton — Frontend

---

## Ordem de execução recomendada

```
1. Arquiteto  →  Contrato API (docs/api.md) + este planning         [CONCLUÍDO]
               ↓
2. ┌─ DBA      →  Auditoria schema (sem novas migrations)
   ├─ Backend  →  DashboardService + BudgetService + entidades       } paralelo
   └─ Frontend →  módulo dashboard + mock data (sem API real)
               ↓
3. ┌─ Backend  →  DashboardController + BudgetController             } paralelo
   ├─ Frontend →  integração real com API                            } paralelo
   └─ QA       →  testes unitários DashboardService + BudgetService
               ↓
4. ┌─ QA       →  testes de integração (endpoints reais)
   └─ Revisor  →  revisão das branches s05-backend, s05-frontend, s05-tests
               ↓
5. DevOps  →  merges para master + docker compose up --build + tag sprint-05
```

---

## Pontos de risco e atenções

### Risco 1 — Query de agregação mista (parcelados vs. à vista)
**Nível:** Alto
**Descrição:** A query de `totalExpense` precisa unir duas fontes:
  (a) `installment.amount` WHERE `installment.reference_month = yyyy-MM-01` (parcelados);
  (b) `transaction.amount` WHERE `transaction.date` no mês E `installmentsTotal = 1` (à vista).
  Uma única query SQL com UNION é mais segura que duas queries separadas somadas no Java,
  pois evita dupla contagem se a lógica de filtro divergir.
**Ação:** Backend deve escrever um teste de integração específico com os dois tipos no mesmo mês.

### Risco 2 — Divisão por zero em variação percentual
**Nível:** Médio
**Descrição:** Se o mês anterior não tem receita (totalIncome = 0) ou não tem despesa,
  a fórmula de variação divide por zero.
**Decisão tomada:** retornar `null` para o campo; frontend não exibe o componente de variação.

### Risco 3 — Optimistic locking no Budget
**Nível:** Médio
**Descrição:** Dois usuários podem editar o mesmo orçamento simultaneamente.
**Decisão tomada:** `Budget` deve ter campo `@Version version` (integer). PUT obriga envio
  do `version` e retorna 409 se desatualizado. Mesmo padrão já adotado em Transaction e Category.

### Risco 4 — Recorrência (RecurringRule) fora de escopo
**Nível:** Baixo (decisão de escopo)
**Descrição:** O `epics-e-sprints.md` previa RecurringRule no Sprint 05 junto do Budget.
  O volume de trabalho (Dashboard + Budget) já justifica o sprint inteiro.
**Decisão tomada:** RecurringRule movida para Sprint 06 (junto com Acerto de Contas).
  API de recorrência a ser detalhada no planning do Sprint 06.

### Risco 5 — Biblioteca de gráficos no frontend
**Nível:** Baixo
**Descrição:** O projeto não define explicitamente qual biblioteca de gráficos usar.
**Decisão tomada:** usar `Chart.js` com wrapper `ng2-charts` (mais comum no ecossistema Angular).
  Se o projeto já tiver outra biblioteca instalada, o frontend deve adaptá-la ao invés de
  adicionar nova dependência.

---

## Definition of Done do Sprint 05

- [ ] `./mvnw test` → BUILD SUCCESS na branch `feature/s05-backend`
- [ ] `./mvnw test` → BUILD SUCCESS na branch `feature/s05-tests`
- [ ] `npm run build` → sem erros na branch `feature/s05-frontend`
- [ ] Agente revisor aprovou todas as branches (sem item bloqueante)
- [ ] DevOps validou `docker compose up --build` com as branches mergeadas
- [ ] Tag `sprint-05` criada no master após merge
- [ ] `docs/api.md` atualizado e coerente com a implementação
