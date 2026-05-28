# Sprint 03
**Período:** Semanas 5–6
**Epic:** Cartões e Parcelamento (parte 1)
**Fatia:** 3a
**Objetivo:** Cadastrar cartões de crédito com fechamento e vencimento; selecionar no lançamento.
**Status:** 🔵 EM ANDAMENTO

---

## Tarefas paralelas — início do sprint

```
DIA 1–4 (paralelo)
├── Backend:  S-03-01 — API de cartões
└── DBA:      Revisar índices de transaction; migration se necessário

DIA 3–7 (após API cartões)
├── Frontend: S-03-02 — Tela de cartões
└── QA:       Testes de CardService

DIA 6–9 (integração)
└── Frontend: Integrar seleção de cartão no formulário de lançamento

DIA 9–10 (fechamento)
├── Revisor:  Revisão
└── DevOps:   Commit + validação Docker
```

---

## Backlog do sprint

### S-03-00 · Validar schema e índices (DBA)
**Papel:** DBA | **Pontos:** 1 | **Status:** `CONCLUIDO`

| Tarefa | Papel | Status |
|---|---|---|
| Verificar campos e constraints da tabela `card` em V2 | DBA | CONCLUIDO |
| Criar V6__add_transaction_card_index.sql (índice ausente identificado) | DBA | CONCLUIDO |
| Documentar resultado neste arquivo | DBA | CONCLUIDO |

> **Resultado:** tabela `card` em V2 completa (campos, constraints, FKs).
> `idx_transaction_card_id ON transaction(card_id)` criado em V6.
> `./mvnw test` → BUILD SUCCESS (38 testes, 0 falhas). Branch: `feature/s03-schema`.

---

### S-03-01 · API de cartões
**Papel:** Backend | **Pontos:** 3 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade Card + CardRepository | Backend | PENDENTE |
| CardService (CRUD) | Backend | PENDENTE |
| CardController (GET, POST, PUT, DELETE) | Backend | PENDENTE |
| Validar closing_day e due_day entre 1–31 | Backend | PENDENTE |
| Impedir exclusão se cartão tem lançamentos | Backend | PENDENTE |
| Testes de CardService | QA | PENDENTE |

---

### S-03-02 · Tela de cartões
**Papel:** Frontend | **Pontos:** 3 | **Depende de:** S-03-01 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo cards com lista de cartões | Frontend | PENDENTE |
| Formulário de criação/edição de cartão | Frontend | PENDENTE |
| Serviço HTTP CardService | Frontend | PENDENTE |
| Integrar seleção de cartão no formulário de lançamento | Frontend | PENDENTE |
| Mostrar/ocultar campo cartão conforme método de pagamento | Frontend | PENDENTE |

---

## Progresso diário

| Dia | Data | Stories avançadas | Impedimentos |
|---|---|---|---|
| 1 | 2026-05-28 | S-03-00 CONCLUIDO (DBA): V6 índice card_id criado; schema validado | — |
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

- [x] S-03-00: schema validado; V6__add_transaction_card_index.sql criada e aplicada
- [ ] S-03-01: CRUD de cartões; validações de dia corretas; delete bloqueia com 409 se há transações
- [ ] S-03-02: Tela de cartões funcional; seleção integrada no formulário de lançamento
- [ ] Campo cartão visível só quando método = crédito
- [ ] Testes passando: `./mvnw test`
- [ ] Review registrada em `memory/reviews/review-sprint-03.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-03.md`
- [ ] Learnings atualizados em `memory/learnings/`
