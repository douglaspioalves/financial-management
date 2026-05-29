# Checkpoint Arquiteto — 2026-05-29

**Sprint:** 04
**Agente:** Arquiteto
**Status:** Sessão concluída — planning e contrato entregues

---

## Feito nesta sessão

### 1. Contrato da API — `docs/api.md` (seção Fatia 3b)
Substituída a seção placeholder "Fatia 3b — Parcelamentos" pelo contrato completo:

- `POST /api/transactions` atualizado: suporte a `installmentsTotal > 1`; resposta inclui
  lista de `installments` quando parcelado; novos status codes para validações de parcelamento.
- `DELETE /api/transactions/{id}` atualizado: remoção de parcelados agora suportada via cascade.
- `GET /api/transactions/{id}/installments` — novo endpoint; retorna parcelas com `isCurrent`.
- `GET /api/installments?month=yyyy-MM` — novo endpoint; retorna parcelas do mês com dados
  da transação pai (usado pelo acerto de contas do Sprint 06).
- Seção "Lógica de cálculo de reference_month" com tabela de exemplos e regras de arredondamento.

Tabela "Pontos em aberto" atualizada: itens 5 e 6 marcados como CONCLUÍDOS; itens 7 e 8 adicionados.

### 2. Decisões de arquitetura — `docs/decisoes.md`
Quatro novas entradas com data 2026-05-29:
- Regra de reference_month (data ≤ closing_day → mesmo mês; data > closing_day → mês seguinte)
  com tratamento explícito de meses curtos.
- Arredondamento: floor em todas as parcelas; última absorve diferença de centavos.
- Parcelamento restrito a EXPENSE + CREDIT, limite de 2 a 48 parcelas.
- Lançamento à vista no crédito (1x) também gera Installment (uniformiza Sprint 06).

### 3. Planning do sprint — `memory/plannings/planning-sprint-04.md`
Criado com:
- 5 stories (S-04-00 a S-04-04) com pontuação total de 16 pontos.
- 17 casos de teste obrigatórios (TC-01 a TC-17) cobrindo todos os casos de borda.
- Diagrama de paralelismo por dia.
- Tabela de riscos com mitigações.
- Decisões de arquitetura tomadas.
- DoD completo com 17 critérios.

---

## Contratos fechados (Backend e Frontend devem seguir)

1. **reference_month:** `data.dayOfMonth <= closingDay` → 1ª parcela no mesmo mês;
   contrário → mês seguinte. Meses curtos: `Math.min(closingDay, lastDayOfMonth(purchaseDate))`.
2. **Arredondamento:** `floor(total / N)` para cada parcela; última = `total - (N-1) * floor(total/N)`.
3. **isCurrent:** calculado no backend com `referenceMonth.equals(YearMonth.now().atDay(1))`.
4. **Endpoint GET /api/transactions/{id}/installments:** retorna `[]` para transações à vista.
5. **Endpoint GET /api/installments?month=yyyy-MM:** inclui objeto `transaction` com dados da compra pai.
6. **Response POST /api/transactions (parcelado):** inclui array `installments` com todas as parcelas.
7. **Limite de parcelas:** 2 a 48 (inclusive). `installmentsTotal=1` é "à vista" e sempre válido.
8. **Receita (INCOME) não pode ser parcelada** — retorna 400.

---

## Pontos de risco identificados

1. **Arredondamento com BigDecimal:** garantir `RoundingMode.FLOOR` e nunca usar `double`.
2. **Dia = closing_day:** comparação deve ser `<=` (não `<`). TC-09 e TC-04 cobrem esse caso.
3. **Virada de ano:** `YearMonth.plusMonths(1)` em Java lida corretamente; testar TC-01 e TC-02.
4. **Meses curtos:** closing_day=28 em jan (31 dias) → 28 é dia válido → funciona normal.
   Caso crítico: closing_day=31 em fev → usar `Math.min`. TC-10 cobre.
5. **Transação à vista no crédito:** deve gerar 1 Installment, não zero. TC-05 cobre.

---

## Pendente

- S-04-00: DBA confirmar índice (branch: `feature/s04-schema`)
- S-04-01: Backend implementar InstallmentService + endpoints (branch: `feature/s04-backend`)
- S-04-01: QA implementar TC-01 a TC-17 (branch: `feature/s04-tests`)
- S-04-02: Frontend badge + expansão + serviço HTTP (branch: `feature/s04-frontend`)
- S-04-03: Revisor revisar lógica de arredondamento e casos de borda
- S-04-04: DevOps merge + docker compose up --build + tag fatia-3

---

## Próximo passo imediato

**DBA** deve iniciar S-04-00 na branch `feature/s04-schema`:
- Verificar que `idx_installment_reference_month` existe em V2 (já consta — deve apenas confirmar).
- Verificar constraint `chk_installment_reference_month_day` (EXTRACT(DAY FROM reference_month) = 1).
- Verificar `ON DELETE CASCADE` em `fk_installment_transaction`.
- Se tudo confirmado: nenhuma migration nova. Atualizar sprint-04.md e liberar Backend.

**Backend** pode iniciar S-04-01 em paralelo ao DBA (as entidades não precisam de migration nova).
O ponto de atenção é a implementação do `InstallmentService.generateInstallments()` —
os casos de borda (meses curtos, virada de ano, arredondamento) são a parte crítica.

---

## Arquivos criados/modificados nesta sessão

- `docs/api.md` — seção Fatia 3b completa + pontos em aberto atualizados
- `docs/decisoes.md` — 4 novas entradas de 2026-05-29
- `memory/plannings/planning-sprint-04.md` — planning completo
- `memory/checkpoints/2026-05-29-arquiteto.md` — este arquivo
