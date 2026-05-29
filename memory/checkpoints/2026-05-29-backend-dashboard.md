# Checkpoint Backend — 2026-05-29 15:15

## Feito nesta sessão

### S-05-01: API de Dashboard — CONCLUÍDO

**Branch:** `feature/s05-backend-dashboard`  
**Commits:**
1. `feat(dashboard): DTOs e DashboardService com agregações mensais`
2. `feat(dashboard): DashboardController GET /api/dashboard`

**Arquivos criados:**
- `backend/src/main/java/com/gastos/dto/dashboard/CategoryExpenseDTO.java`
- `backend/src/main/java/com/gastos/dto/dashboard/RecentTransactionDTO.java`
- `backend/src/main/java/com/gastos/dto/dashboard/MonthSummaryDTO.java`
- `backend/src/main/java/com/gastos/dto/dashboard/DashboardResponse.java`
- `backend/src/main/java/com/gastos/service/DashboardService.java`
- `backend/src/main/java/com/gastos/controller/DashboardController.java`

**Arquivos modificados:**
- `backend/src/main/java/com/gastos/repository/TransactionRepository.java`
  — adicionadas: `findCashExpensesByMonth`, `findIncomesByMonth`, `findRecentByMonth`
- `backend/src/main/java/com/gastos/repository/InstallmentRepository.java`
  — adicionada: `findExpenseInstallmentsByMonth`

**Resultado dos testes:** BUILD SUCCESS — 70 testes, 0 falhas, 0 erros

### Regras implementadas

- Despesas parceladas (CREDIT com installmentsTotal > 1): agregadas via `Installment.referenceMonth`
- Demais despesas e receitas: por `transaction.date` no mês
- Variação percentual vs mês anterior: null quando mês anterior tem total zero
- Breakdown por categoria: ordenado por total DESC, percentual calculado sobre totalExpense
- Top 10 transações recentes: por `date DESC, createdAt DESC`, limitadas a 10
- Todas as queries usam `JOIN FETCH` para category e paidByPerson (evita N+1)

## Pendente

- Nenhum item pendente para S-05-01.
- Próximas tarefas do Sprint 05: agente QA deve criar `DashboardServiceTest`
  cobrindo os cenários: mês sem dados, mês com apenas parceladas, variação percentual.

## Próximo passo imediato

Agente QA: implementar `DashboardServiceTest` com mocks dos repositórios testando:
1. `calculate()` com mês vazio → todos zeros, previousMonth zeros, variação null
2. `calculate()` com mix de despesas à vista e parceladas → totalExpense correto
3. `expenseByCategory` ordenado por total DESC com percentuais corretos
4. `incomeVariation` e `expenseVariation` null quando mês anterior zerado
5. `recentTransactions` limitado a 10 itens
