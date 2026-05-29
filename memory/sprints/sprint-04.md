# Sprint 04
**Período:** Semanas 7–8
**Epic:** Cartões e Parcelamento (parte 2)
**Fatia:** 3b
**Objetivo:** Gerar parcelas automaticamente ao criar compra parcelada; exibir badges na lista.
**Status:** ✅ CONCLUÍDO

---

## Tarefas paralelas — início do sprint

```
DIA 1–2 (paralelo)
├── DBA:      Confirmar índice em installment.reference_month
└── Arquiteto: Revisar lógica de reference_month e documentar exemplos

DIA 2–7 (núcleo — backend sequencial)
└── Backend:  S-04-01 — InstallmentService + lógica de reference_month

DIA 4–7 (paralelo ao backend)
└── QA:       Preparar casos de teste (virada de ano, fechamento 28, 30, 31)

DIA 6–9 (após backend)
└── Frontend: S-04-02 — Badge e detalhe de parcelas

DIA 9–10 (fechamento)
├── Revisor:  Revisão da lógica de parcelamento
└── DevOps:   Commit + tag fatia-3
```

---

## Backlog do sprint

### S-04-01 · Geração de parcelas
**Papel:** Backend + DBA | **Pontos:** 8 | **Depende de:** S-03-01 | **Status:** `CONCLUIDO`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade Installment + InstallmentRepository | Backend | CONCLUIDO |
| InstallmentService.generateInstallments() | Backend | CONCLUIDO |
| Lógica de cálculo de reference_month por closing_day | Backend | CONCLUIDO |
| Tratamento de virada de ano (dez → jan) | Backend | CONCLUIDO |
| Arredondamento: última parcela absorve diferença de centavos | Backend | CONCLUIDO |
| Chamar generateInstallments() no TransactionService ao criar parcelado | Backend | CONCLUIDO |
| Endpoint GET /api/transactions/{id}/installments | Backend | CONCLUIDO |
| Confirmar índice em installment.reference_month | DBA | CONCLUIDO |
| Teste: N parcelas no mês certo | QA | CONCLUIDO |
| Teste: arredondamento (valor não divide exato) | QA | CONCLUIDO |
| Teste: virada de ano (compra em nov, 3x → nov/dez/jan) | QA | CONCLUIDO |
| Teste: closing_day 28/30/31 em meses curtos | QA | CONCLUIDO |

---

### S-04-02 · Visualização de parcelas
**Papel:** Frontend | **Pontos:** 4 | **Depende de:** S-04-01 | **Status:** `CONCLUIDO`

| Tarefa | Papel | Status |
|---|---|---|
| Badge "X/N" no componente de lista de lançamentos | Frontend | CONCLUIDO |
| Componente de expansão com lista de parcelas | Frontend | CONCLUIDO |
| Destaque visual na parcela do mês atual | Frontend | CONCLUIDO |
| Serviço HTTP InstallmentService | Frontend | CONCLUIDO |

---

## Observações técnicas

- **DBA:** nenhuma migration nova necessária. Índices em `installment.reference_month`
  e `installment.transaction_id` já existem em V2__initial_schema.sql.
  ON DELETE CASCADE em `installment.transaction_id` também já está em V2.
- **Backend:** `TransactionService.create()` lança exceção placeholder para `installmentsTotal > 1`;
  remover o placeholder e implementar chamada a `InstallmentService.generateInstallments()`.
- **Backend:** ajustar `TransactionResponse` para incluir objeto `card` (id + name) em vez
  de apenas `cardId` (UUID simples). Ver ponto #8 na tabela de pontos em aberto do api.md.
- **Contrato canônico:** algoritmo de reference_month documentado em
  `memory/decisions/2026-05-28-installment-reference-month-algorithm.md`.

---

## Progresso diário

| Dia | Data | Stories avançadas | Impedimentos |
|---|---|---|---|
| 1 | 2026-05-28 | Planning concluído; contrato da API Fatia 3b definido; decisão de algoritmo registrada | — |
| 2 | 2026-05-29 | S-04-01 (Backend: InstallmentService, entidade, endpoint); S-04-01 (DBA: schema V7); fix R-02 (closingDay meses curtos); S-04-01 (QA: 12 casos de borda + 5 integração); S-04-02 (Frontend: badge X/N + expansion panel + InstallmentService) | nenhum |
| 3 | 2026-05-29 | Sprint fechado: merges para master, tag sprint-04 criada, 70 testes passando | — |
| 4 | — | — | — |
| 5 | — | — | — |
| 6 | — | — | — |
| 7 | — | — | — |
| 8 | — | — | — |
| 9 | — | — | — |
| 10 | — | — | — |

---

## Definition of Done

- [x] S-04-01: Compra parcelada gera N Installments; reference_month correto em todos os casos de borda
- [x] S-04-01: Soma das parcelas = valor total da compra
- [x] S-04-02: Badge visível na lista; expansão exibe todas as parcelas
- [x] Todos os testes de parcelamento passando (70/70 BUILD SUCCESS)
- [ ] `docker compose up --build` limpo
- [x] Tag `sprint-04` criada no git (push pendente: HTTP 403 no proxy local)
- [x] Review registrada em `memory/reviews/review-sprint-04.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-04.md`
- [ ] Learnings atualizados em `memory/learnings/`
