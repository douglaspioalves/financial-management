# Checkpoint Backend — 2026-05-29

## Feito nesta sessão

### S-06-03: API de Acerto de Contas — CONCLUÍDA

**Branch:** `feature/s06-backend-settlement`

**Arquivos criados/modificados:**

1. `backend/src/main/java/com/gastos/dto/settlement/PersonSettlementDTO.java`
   - Record com: id, name, totalPaid, shouldPay, balance

2. `backend/src/main/java/com/gastos/dto/settlement/SettlementResponse.java`
   - Record com: month, totalExpense, personA, personB, debtor, creditor, amountOwed, settled, pendingProportional, pendingMessage

3. `backend/src/main/java/com/gastos/service/SettlementService.java`
   - Algoritmo completo em 5 passos (agregação, divisão, proporção, pagamentos, resultado)
   - Suporte a todos os SplitRules: FIFTY_FIFTY, PERSON_A, PERSON_B, PROPORTIONAL
   - PROPORTIONAL usa receitas individuais do mês (INCOME + PERSON_A/PERSON_B)
   - Pendência corretamente sinalizada quando não há receitas individuais cadastradas
   - BigDecimal com RoundingMode.HALF_UP em todos os cálculos

4. `backend/src/main/java/com/gastos/controller/SettlementController.java`
   - GET /api/settlement?month=yyyy-MM
   - Parâmetro month opcional (default: mês corrente)

5. `backend/src/main/java/com/gastos/repository/TransactionRepository.java`
   - Adicionado `findIndividualIncomesByMonth()` para receitas INCOME + PERSON_A/PERSON_B

**Status dos testes:**
- `mvn test` → BUILD SUCCESS (102 tests, 0 failures, 0 errors, 17 skipped)

**Commits na branch:**
- feat(settlement): adiciona DTOs PersonSettlementDTO e SettlementResponse
- feat(settlement): adiciona findIndividualIncomesByMonth ao TransactionRepository
- feat(settlement): implementa SettlementService com algoritmo completo de acerto
- feat(settlement): implementa SettlementController — GET /api/settlement?month=yyyy-MM
- fix(settlement): corrige UnsupportedOperationException ao ordenar persons

## Pendente
- Nada. A implementação está completa e testada.
- QA deve escrever testes de integração para `SettlementService` (S-06-03 QA tasks)

## Próximo passo imediato
Aguardar merge do branch pelo DevOps após revisão.
