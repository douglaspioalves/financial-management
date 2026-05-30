# Sprint 06
**Período:** Semanas 11–12
**Epic:** Recorrência + Acerto de Contas
**Fatias:** 5b e 6
**Objetivo:** Lançamentos recorrentes automáticos e cálculo completo do acerto de contas com proporção dinâmica.
**Status:** 🟢 EM ANDAMENTO — backends concluídos, frontends em progresso

---

## Tarefas paralelas — início do sprint

```
DIA 1–4 (paralelo)
├── Backend:  S-06-01 — API de recorrência (job agendado)
└── Arquiteto: Definir comportamento de mês já acertado (decisão pendente)
              → registrar em memory/decisions/

DIA 2–7 (núcleo mais complexo do projeto)
└── Backend:  S-06-03 — SettlementService (acerto de contas)

DIA 3–6 (paralelo ao acerto)
└── QA:       Preparar todos os casos de teste do acerto
              (50/50, proporcional, sem receita, só de uma pessoa)

DIA 5–9 (após backends)
├── Frontend: S-06-02 — Tela de recorrências
└── Frontend: S-06-04 — Tela de acerto de contas

DIA 9–10 (fechamento)
├── Revisor:  Revisão aprofundada do SettlementService
└── DevOps:   Commit + tag fatia-5 e fatia-6
```

---

## Backlog do sprint

### S-06-01 · API de lançamentos recorrentes
**Papel:** Backend | **Pontos:** 5 | **Status:** `✅ CONCLUÍDO` | **Branch:** `feature/s06-backend-recurring`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade RecurringRule + RecurringRuleRepository | Backend | ✅ |
| RecurringRuleService (criar, listar, desativar) | Backend | ✅ |
| Job agendado @Scheduled para gerar lançamentos na data certa | Backend | ✅ |
| Garantir idempotência (não duplicar se job rodar 2x no mesmo dia) | Backend | ✅ |
| Endpoints GET/POST/DELETE /api/recurring-rules | Backend | ✅ |
| Testes do job (geração na data, sem duplicar) | QA | 🟡 integração @Disabled aguarda merge |

---

### S-06-02 · Tela de recorrências
**Papel:** Frontend | **Pontos:** 3 | **Depende de:** S-06-01 | **Status:** `🟡 EM PROGRESSO` | **Branch:** `feature/s06-frontend-recurring`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo de recorrências com lista | Frontend | 🟡 |
| Formulário de criação (frequência, template do lançamento) | Frontend | 🟡 |
| Ação de desativar recorrência | Frontend | 🟡 |

---

### S-06-03 · API de acerto de contas
**Papel:** Backend | **Pontos:** 8 | **Depende de:** S-04-01 | **Status:** `✅ CONCLUÍDO` | **Branch:** `feature/s06-backend-settlement`

| Tarefa | Papel | Status |
|---|---|---|
| SettlementService.calculate(YearMonth) | Backend | ✅ |
| Agregar Installments do mês + lançamentos à vista | Backend | ✅ |
| Lógica FIFTY_FIFTY: split 50/50 de cada despesa | Backend | ✅ |
| Lógica PERSON_A / PERSON_B: 100% para a pessoa | Backend | ✅ |
| Lógica PROPORTIONAL: buscar receitas PERSON_A/B do mês | Backend | ✅ |
| PROPORTIONAL: calcular proporção (ex: 6000/10000 = 60%) | Backend | ✅ |
| PROPORTIONAL: mês sem receita → pendingProportional: true | Backend | ✅ |
| Resultado final: debtor/creditor/amountOwed ou settled=true | Backend | ✅ |
| Endpoint GET /api/settlement?month=yyyy-MM | Backend | ✅ |
| Decidir comportamento de mês já acertado → memory/decisions/ | Arquiteto | ✅ Opção A (stateless) |
| Teste TC-S01: FIFTY_FIFTY simples | QA | ✅ |
| Teste TC-S02: PERSON_A (settled) | QA | ✅ |
| Teste TC-S03: PERSON_B (settled) | QA | ✅ |
| Teste TC-S04: PROPORTIONAL com receitas | QA | ✅ |
| Teste TC-S05: PROPORTIONAL sem receita → pending | QA | ✅ |
| Teste TC-S06: MIX FIFTY_FIFTY + PROPORTIONAL sem receita | QA | ✅ |
| Teste TC-S07: mês sem despesas | QA | ✅ |
| Teste TC-S08: parcela de cartão no acerto | QA | ✅ |
| Teste TC-S09: receita FIFTY_FIFTY excluída da proporção | QA | ✅ |
| Teste TC-S10: arredondamento HALF_UP | QA | ✅ |

---

### S-06-04 · Tela de acerto de contas
**Papel:** Frontend | **Pontos:** 5 | **Depende de:** S-06-03 | **Status:** `🟡 EM PROGRESSO` | **Branch:** `feature/s06-frontend-settlement`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo settlement com layout de acerto | Frontend | 🟡 |
| Exibição de "X deve R$ Y a Z" com destaque visual | Frontend | 🟡 |
| Alerta de proporção pendente ("Cadastre as receitas de [mês]") | Frontend | 🟡 |
| Breakdown por pessoa: pagou vs. deveria pagar | Frontend | 🟡 |
| Navegação por mês | Frontend | 🟡 |
| Serviço HTTP SettlementService | Frontend | 🟡 |

---

## Progresso diário

| Dia | Data | Stories avançadas | Impedimentos |
|---|---|---|---|
| 1 | — | — | — |
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

- [ ] S-06-01: Job gera lançamentos na data; sem duplicação
- [ ] S-06-03: Todos os cenários de acerto corretos (50/50, proporcional, pending)
- [ ] S-06-04: Tela exibe acerto; alerta de pendência funcional
- [ ] Decisão sobre mês retroativo registrada em memory/decisions/
- [ ] Todos os testes do SettlementService passando
- [ ] `docker compose up --build` limpo
- [ ] Tags `fatia-5` e `fatia-6` criadas no git
- [ ] Review registrada em `memory/reviews/review-sprint-06.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-06.md`
- [ ] Learnings atualizados em `memory/learnings/`
