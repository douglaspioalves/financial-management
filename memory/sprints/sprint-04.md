# Sprint 04
**Período:** Semanas 7–8
**Epic:** Cartões e Parcelamento (parte 2)
**Fatia:** 3b
**Objetivo:** Gerar parcelas automaticamente ao criar compra parcelada; exibir badges na lista.
**Status:** 🟢 EM ANDAMENTO

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
**Papel:** Backend + DBA | **Pontos:** 8 | **Depende de:** S-03-01 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade Installment + InstallmentRepository | Backend | PENDENTE |
| InstallmentService.generateInstallments() | Backend | PENDENTE |
| Lógica de cálculo de reference_month por closing_day | Backend | PENDENTE |
| Tratamento de virada de ano (dez → jan) | Backend | PENDENTE |
| Arredondamento: última parcela absorve diferença de centavos | Backend | PENDENTE |
| Chamar generateInstallments() no TransactionService ao criar parcelado | Backend | PENDENTE |
| Endpoint GET /api/transactions/{id}/installments | Backend | PENDENTE |
| Confirmar índice em installment.reference_month | DBA | PENDENTE |
| Teste: N parcelas no mês certo | QA | PENDENTE |
| Teste: arredondamento (valor não divide exato) | QA | PENDENTE |
| Teste: virada de ano (compra em nov, 3x → nov/dez/jan) | QA | PENDENTE |
| Teste: closing_day 28/30/31 em meses curtos | QA | PENDENTE |

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
| 1 | 2026-05-29 | S-04-02 (Frontend: badge + expansion panel + InstallmentService) | nenhum |
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

- [ ] S-04-01: Compra parcelada gera N Installments; reference_month correto em todos os casos de borda
- [ ] S-04-01: Soma das parcelas = valor total da compra
- [x] S-04-02: Badge visível na lista; expansão exibe todas as parcelas
- [ ] Todos os testes de parcelamento passando
- [ ] `docker compose up --build` limpo
- [ ] Tag `fatia-3` criada no git
- [ ] Review registrada em `memory/reviews/review-sprint-04.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-04.md`
- [ ] Learnings atualizados em `memory/learnings/`
