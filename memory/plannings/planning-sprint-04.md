# Planning — Sprint 04
**Data:** 2026-05-29
**Sprint:** 04 de 07
**Período:** Semanas 7–8
**Fatia do roadmap:** 3b — Parcelamentos (geração automática de Installment)
**Conduzido por:** Scrum Master / Arquiteto

---

## Objetivo do sprint

Ao fim do sprint, o usuário poderá criar uma compra parcelada no cartão de crédito
e o sistema gerará automaticamente as N parcelas nos meses corretos, calculados com base
no dia de fechamento do cartão. A lista de lançamentos exibirá um badge "X/N" nas compras
parceladas, e o usuário poderá expandir para ver todas as parcelas com destaque no mês atual.

---

## Contexto técnico

- **Schema:** tabela `installment` já existe em V2__initial_schema.sql com as colunas
  `id`, `transaction_id`, `number`, `amount`, `reference_month` (DATE, dia sempre = 1).
  Índices `idx_installment_reference_month` e `idx_installment_transaction_id` já criados.
  Constraint `ON DELETE CASCADE` em `fk_installment_transaction` garante remoção em cascata.
- **Migration necessária:** nenhuma. O DBA deve apenas confirmar que o índice
  `idx_installment_reference_month` está presente (já consta em V2).
- **Entidades existentes mergeadas em master:** User, Person, Category, Card, Transaction
  com todos os seus repositórios, serviços e controllers.
- **Restrição removida:** o bloqueio de `installmentsTotal > 1` do Sprint 02 deve ser
  removido do `TransactionService` ao implementar S-04-01.
- **Contrato da API:** ver `docs/api.md` seção "Fatia 3b — Parcelamentos".

---

## Stories selecionadas

| ID | Story | Papel | Pontos | Depende de |
|---|---|---|---|---|
| S-04-00 | DBA: confirmar índice installment.reference_month | DBA | 1 | — |
| S-04-01 | Backend: geração automática de parcelas | Backend + QA | 8 | S-04-00 |
| S-04-02 | Frontend: badge e detalhe de parcelas | Frontend | 4 | S-04-01 |
| S-04-03 | Revisor: revisão da lógica de parcelamento | Revisor | 2 | S-04-01 |
| S-04-04 | DevOps: merge + tag fatia-3 | DevOps | 1 | S-04-02, S-04-03 |

**Total de pontos:** 16

---

## Detalhamento das stories

### S-04-00 · DBA — Confirmar índice reference_month
**Papel:** DBA | **Pontos:** 1 | **Branch:** `feature/s04-schema`

| Tarefa | Responsável | Status |
|---|---|---|
| Verificar que `idx_installment_reference_month` existe em V2 | DBA | PENDENTE |
| Confirmar constraint `chk_installment_reference_month_day` (dia=1) | DBA | PENDENTE |
| Verificar constraint `ON DELETE CASCADE` em `fk_installment_transaction` | DBA | PENDENTE |
| Rodar `./mvnw test` para confirmar BUILD SUCCESS | DBA | PENDENTE |
| Atualizar sprint-04.md: S-04-00 CONCLUIDO | DBA | PENDENTE |

**Critérios de aceite:**
- Índice confirmado presente (sem necessidade de nova migration).
- Se ausente (não esperado): criar `V7__add_installment_indexes.sql`.

---

### S-04-01 · Backend — Geração de parcelas
**Papel:** Backend + QA | **Pontos:** 8 | **Branch:** `feature/s04-backend`
**Depende de:** S-04-00

| Tarefa | Responsável | Status |
|---|---|---|
| Criar entidade `Installment.java` (JPA, sem `@Version` próprio) | Backend | PENDENTE |
| Criar `InstallmentRepository.java` | Backend | PENDENTE |
| Criar `InstallmentService.generateInstallments(transaction, card)` | Backend | PENDENTE |
| Implementar lógica de `reference_month` com base em `closing_day` | Backend | PENDENTE |
| Implementar tratamento de virada de ano (dez → jan) | Backend | PENDENTE |
| Implementar tratamento de closing_day em meses curtos (ex.: 31 em fev) | Backend | PENDENTE |
| Implementar arredondamento: última parcela absorve diferença | Backend | PENDENTE |
| Remover bloqueio de `installmentsTotal > 1` do `TransactionService` | Backend | PENDENTE |
| Chamar `generateInstallments()` no `TransactionService.create()` quando `installmentsTotal > 1` | Backend | PENDENTE |
| Incluir lista de parcelas na resposta do `POST /api/transactions` (quando parcelado) | Backend | PENDENTE |
| Implementar `GET /api/transactions/{id}/installments` | Backend | PENDENTE |
| Implementar `GET /api/installments?month=yyyy-MM` | Backend | PENDENTE |
| Atualizar `DELETE /api/transactions/{id}`: remover bloqueio para parcelados (cascade resolve) | Backend | PENDENTE |
| Testes unitários: InstallmentServiceTest (ver casos abaixo) | QA | PENDENTE |
| Testes de integração: InstallmentIntegrationTest | QA | PENDENTE |

**Casos de teste obrigatórios (QA):**

| ID | Cenário | Esperado |
|---|---|---|
| TC-01 | Compra 10/11, closing=15, 3x | Parcelas nov/2026, dez/2026, jan/2027 |
| TC-02 | Compra 20/11, closing=15, 3x | Parcelas dez/2026, jan/2027, fev/2027 |
| TC-03 | Compra 31/01, closing=28, 2x | Parcelas fev/2027, mar/2027 |
| TC-04 | Compra 28/12, closing=28, 2x | Parcelas dez/2026, jan/2027 |
| TC-05 | Compra 10/11, closing=15, 1x (crédito à vista) | 1 parcela nov/2026 |
| TC-06 | Valor R$100,01 em 3x | Parcelas R$33,33 / R$33,33 / R$33,35 |
| TC-07 | Valor R$10,00 em 3x | Parcelas R$3,33 / R$3,33 / R$3,34 |
| TC-08 | Valor R$100,00 em 4x | 4 parcelas de R$25,00 |
| TC-09 | Compra 15/12, closing=15, 3x | Parcelas dez/2026, jan/2027, fev/2027 (dia=closing → mesmo mês) |
| TC-10 | Compra em fev, closing=31, 2x | Parcela 1 em fev (closing efetivo = último dia do mês) |
| TC-11 | GET /api/transactions/{id}/installments — transação parcelada | Retorna N parcelas com isCurrent correto |
| TC-12 | GET /api/transactions/{id}/installments — transação à vista | Retorna [] |
| TC-13 | GET /api/installments?month=2026-11 | Retorna só parcelas do mês, com dados da transação pai |
| TC-14 | DELETE transação parcelada | 204; parcelas removidas (verificar no banco) |
| TC-15 | POST parcelado com paymentMethod!=CREDIT | 400 — "Parcelamento só é permitido para pagamentos com cartão de crédito." |
| TC-16 | POST parcelado com type=INCOME | 400 — "Receitas não podem ser parceladas." |
| TC-17 | POST parcelado com installmentsTotal=49 | 400 — "O número de parcelas deve estar entre 2 e 48." |

**Critérios de aceite:**
- Compra parcelada gera N `Installment`; `reference_month` correto em todos os casos de borda.
- Soma das parcelas = valor total da compra (validado nos TC-06, TC-07, TC-08).
- `GET /api/transactions/{id}/installments` retorna lista com `isCurrent` correto.
- `GET /api/installments?month=yyyy-MM` retorna dados da transação pai.
- `DELETE` de transação parcelada remove cascateia `Installment`.
- Todos os 17 casos de teste passando.

---

### S-04-02 · Frontend — Badge e detalhe de parcelas
**Papel:** Frontend | **Pontos:** 4 | **Branch:** `feature/s04-frontend`
**Depende de:** S-04-01 (endpoint disponível)

| Tarefa | Responsável | Status |
|---|---|---|
| Serviço HTTP `InstallmentService` (Angular) — `getByTransaction(id)` e `getByMonth(month)` | Frontend | PENDENTE |
| Badge "X/N" no componente de lista de lançamentos para `installmentsTotal > 1` | Frontend | PENDENTE |
| Componente de expansão/accordion com lista de parcelas (número, mês, valor) | Frontend | PENDENTE |
| Destaque visual (cor de fundo diferente) na parcela com `isCurrent = true` | Frontend | PENDENTE |
| Campo `installmentsTotal` habilitado no formulário de novo lançamento (1 a 48) | Frontend | PENDENTE |
| `npm run build` sem erros | Frontend | PENDENTE |

**Critérios de aceite:**
- Badge "X/N" visível para lançamentos parcelados; ausente para lançamentos à vista.
- Expansão exibe todas as parcelas ordenadas por número.
- Parcela do mês atual visualmente destacada.
- Formulário de lançamento permite selecionar número de parcelas de 2 a 48 quando
  `paymentMethod = CREDIT`.

---

### S-04-03 · Revisor — Revisão da lógica de parcelamento
**Papel:** Revisor | **Pontos:** 2
**Depende de:** S-04-01

| Tarefa | Status |
|---|---|
| Revisar `InstallmentService`: casos de borda (virada de ano, meses curtos) | PENDENTE |
| Verificar que `BigDecimal` é usado em todos os cálculos monetários | PENDENTE |
| Verificar que nenhuma entidade JPA é exposta diretamente no controller | PENDENTE |
| Verificar mensagens de erro em pt-br | PENDENTE |
| Verificar autenticação obrigatória nos novos endpoints | PENDENTE |
| Registrar resultado em `memory/reviews/review-sprint-04.md` | PENDENTE |

---

### S-04-04 · DevOps — Merge e tag
**Papel:** DevOps | **Pontos:** 1
**Depende de:** S-04-02, S-04-03

| Tarefa | Status |
|---|---|
| Merge `feature/s04-schema` → master (--no-ff) | PENDENTE |
| Merge `feature/s04-backend` → master (--no-ff) | PENDENTE |
| Merge `feature/s04-tests` → master (--no-ff) | PENDENTE |
| Merge `feature/s04-frontend` → master (--no-ff) | PENDENTE |
| Rodar `docker compose up --build` e validar fluxo completo | PENDENTE |
| Criar tag `fatia-3` no git | PENDENTE |

---

## Tarefas paralelas identificadas

```
DIA 1 (paralelo):
├── DBA:      S-04-00 — confirmar índice reference_month (branch: feature/s04-schema)
└── Arquiteto: docs/api.md Fatia 3b + planning-sprint-04.md + decisoes.md (branch: docs/s04-planning)

DIA 2–7 (núcleo sequencial):
└── Backend:  S-04-01 — InstallmentService + endpoints (branch: feature/s04-backend)

DIA 2–5 (paralelo ao backend):
└── QA:       Preparar e implementar casos de teste TC-01 a TC-17 (branch: feature/s04-tests)

DIA 6–9 (após backend):
└── Frontend: S-04-02 — Badge + expansão + formulário (branch: feature/s04-frontend)

DIA 8–9 (paralelo):
└── Revisor:  S-04-03 — Revisão da lógica de parcelamento

DIA 10 (fechamento):
└── DevOps:   S-04-04 — Merges + docker compose up --build + tag fatia-3
```

---

## Riscos e pontos de atenção

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Arredondamento com BigDecimal: uso incorreto de `double` em divisão | Médio | Alto (centavos errados) | Code review obrigatório no `InstallmentService`; TC-06, TC-07, TC-08 |
| Virada de ano: `YearMonth.plusMonths(1)` deve funcionar corretamente | Baixo | Alto (parcelas em mês errado) | TC-01, TC-02 cobrem nov→jan |
| Meses curtos: closing_day=31 em fevereiro | Médio | Médio (NullPointer ou parcela errada) | TC-10 cobre o caso |
| Dia=closing_day: comparação deve ser ≤ não < | Alto (pegadinha) | Alto | TC-09 cobre o caso exato |
| Concorrência: dois usuários criando parcelas simultaneamente | Baixo | Baixo (constraint uq_installment_transaction_number detecta) | Já coberto por constraint no banco |
| Frontend exibindo `installmentsTotal` de transações antigas (Sprint 02) com valor 1 | Baixo | Baixo (badge não aparece = correto) | Badge condicional em `installmentsTotal > 1` |

---

## Decisões de arquitetura tomadas neste sprint

1. **Lógica de reference_month:** data ≤ closing_day → mesmo mês; data > closing_day → mês seguinte.
   Meses curtos: `Math.min(closingDay, lastDayOfMonth)`. Ver `docs/decisoes.md`.

2. **Arredondamento:** floor em todas as parcelas exceto a última (absorve diferença).
   Implementar com `BigDecimal.ROUND_FLOOR` / `setScale(2, RoundingMode.FLOOR)`.

3. **Lançamento à vista no crédito (1x) também gera Installment.**
   Uniformiza o acerto de contas do Sprint 06.

4. **Parcelamento restrito a EXPENSE + CREDIT, máx. 48 parcelas.**

5. **`isCurrent` calculado no backend** (baseado em `LocalDate.now()`), não no frontend.

---

## Definição de pronto (DoD) do sprint

- [ ] S-04-00: índice confirmado; sem migration nova necessária
- [ ] S-04-01: `POST /api/transactions` com `installmentsTotal > 1` gera N Installments
- [ ] S-04-01: `reference_month` correto em todos os 17 casos de teste
- [ ] S-04-01: soma das parcelas = valor total da compra
- [ ] S-04-01: `GET /api/transactions/{id}/installments` implementado com `isCurrent`
- [ ] S-04-01: `GET /api/installments?month=yyyy-MM` implementado com dados da transação pai
- [ ] S-04-01: `DELETE /api/transactions/{id}` remove cascateia parcelas
- [ ] S-04-02: badge "X/N" visível na lista de lançamentos
- [ ] S-04-02: componente de expansão com lista de parcelas
- [ ] S-04-02: destaque visual na parcela do mês atual
- [ ] Todos os testes passando (`./mvnw test` → BUILD SUCCESS)
- [ ] `npm run build` sem erros
- [ ] `docker compose up --build` limpo com fluxo completo validado
- [ ] Tag `fatia-3` criada no git
- [ ] Review registrada em `memory/reviews/review-sprint-04.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-04.md`
- [ ] Learnings atualizados em `memory/learnings/`
