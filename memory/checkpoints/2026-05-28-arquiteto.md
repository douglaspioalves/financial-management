# Checkpoint Arquiteto — 2026-05-28 (sessão 2 — Sprint 04 Planning)

**Sprint:** 04
**Agente:** Arquiteto
**Status:** Sessão concluída — planning, contrato e decisão de algoritmo entregues

---

## Feito nesta sessão (Sprint 04 Planning)

### 1. Decisão de algoritmo — `memory/decisions/2026-05-28-installment-reference-month-algorithm.md`

Documentado o algoritmo canônico de cálculo de `reference_month` para parcelas:

- **Lógica:** se `purchaseDate.dayOfMonth < closingDay` → mesmo mês; senão → mês seguinte.
- **Java:** `YearMonth.from(purchaseDate).plusMonths(...)` lida com virada de ano automaticamente.
- **Arredondamento:** `RoundingMode.DOWN` nas N-1 primeiras parcelas; última absorve a diferença.
- **Exemplos canônicos** documentados (TC-01 a TC-05 e arredondamento TC-06 a TC-09).
- **Caso especial closing_day >= 29:** sem tratamento especial necessário; algoritmo correto.

### 2. Contrato da API — `docs/api.md` (seção Fatia 3b completa)

Adicionada a seção Fatia 3b com:
- `POST /api/transactions` atualizado: parcelamento habilitado, validações adicionais,
  bloqueio de edição (422), delete habilitado (cascade).
- `GET /api/transactions/{id}/installments`: novo endpoint, response com lista de
  `InstallmentResponse` (id, number, amount, referenceMonth), 200 para vazio (à vista), 404 se transação inexistente.
- Seção "Contrato fechado" com 4 regras que Backend e Frontend devem seguir.
- Campo `card: { id, name }` substituindo `cardId: UUID` no `TransactionResponse`.
- Tabela de pontos em aberto atualizada com itens 5–9 (5 e 6 marcados CONCLUÍDO, 7–9 do Sprint 04).
- Fatias 4, 5, 6 com números de sprint corrigidos (05, 06, 07).

### 3. Sprint 04 — `memory/sprints/sprint-04.md`

- Status: 🔵 EM ANDAMENTO
- Dia 1: 2026-05-28 — planning concluído
- Observações técnicas adicionadas: sem migration nova; algoritmo de referência;
  placeholder do TransactionService; ajuste em TransactionResponse.

### 4. Planning — `memory/plannings/planning-sprint-04.md`

Criado com:
- Objetivo do sprint
- Contexto técnico (schema V2 completo, sem migration nova)
- 5 stories: S-04-00 (DBA, sem commit), S-04-01 (Backend), S-04-02 (QA), S-04-03 (Frontend), S-04-04 (Revisor+DevOps)
- Detalhamento de tarefas com código Java do algoritmo para o Backend
- 20 casos de teste (TC-01 a TC-20) para o QA
- Diagrama de paralelismo por dia
- Tabela de riscos (6 riscos identificados)
- DoD completo (13 itens)

### 5. Daily — `memory/dailies/2026-05-28.md`

Adicionada seção Sprint 04 — Dia 1 com registro da sessão do Arquiteto.

---

## Contratos fechados (Backend e Frontend devem seguir)

1. **Algoritmo de reference_month:** baseado em `closingDay`. Dia compra < closingDay → mesmo mês; >= closingDay → mês seguinte. Sempre dia 1 do mês resultado.
2. **Arredondamento:** `RoundingMode.DOWN` nas N-1 parcelas; última = totalAmount - (base × (N-1)).
3. **Limite de parcelas:** `installmentsTotal` entre 1 e 48 (validação Bean Validation).
4. **Parcelado exige CREDIT + cardId:** validação no service com mensagens em pt-br.
5. **PUT bloqueado para parcelados:** retorna 422. DELETE habilitado (CASCADE no banco).
6. **GET /api/transactions/{id}/installments:** 200 com lista vazia para à vista; 404 se transação inexistente.
7. **TransactionResponse.card:** objeto `{ id, name }` em vez de `cardId: UUID`. Frontend deve atualizar uso.
8. **Lançamentos parcelados:** sem botão "Editar" no frontend. Campo "Parcelas" visível apenas para CREDIT.

---

## Pendente

- S-04-00: DBA confirma schema `installment` em V2 (sem migration nova esperada)
- S-04-01: Backend implementa `Installment` entity + `InstallmentService` + ajustes em `TransactionService`/`TransactionResponse` (branch: `feature/s04-backend`)
- S-04-02: QA cobre 20 casos de teste (branch: `feature/s04-tests`)
- S-04-03: Frontend badge + expansão + campo parcelas (branch: `feature/s04-frontend`)
- S-04-04: Revisor + DevOps — merges + tag `fatia-3`

---

## Próximo passo imediato

DBA deve executar S-04-00: confirmar que `V2__initial_schema.sql` contém a tabela
`installment` com ON DELETE CASCADE em `transaction_id`. Sem migration nova esperada.

Após confirmação do DBA, Backend inicia S-04-01 na branch `feature/s04-backend`.

---

## Arquivos criados/modificados nesta sessão

- `docs/api.md` — seção Fatia 3b completa; pontos em aberto atualizados; numeração de sprints corrigida
- `memory/decisions/2026-05-28-installment-reference-month-algorithm.md` — algoritmo canônico (novo)
- `memory/sprints/sprint-04.md` — status EM ANDAMENTO, Dia 1 preenchido, observações técnicas
- `memory/plannings/planning-sprint-04.md` — planning completo (novo)
- `memory/dailies/2026-05-28.md` — seção Sprint 04 adicionada
- `memory/checkpoints/2026-05-28-arquiteto.md` — este arquivo (sobrescrito)
