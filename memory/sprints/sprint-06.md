# Sprint 06
**Período:** Semanas 11–12
**Epic:** Recorrência + Acerto de Contas
**Fatias:** 5b e 6
**Objetivo:** Lançamentos recorrentes automáticos e cálculo completo do acerto de contas com proporção dinâmica.
**Status:** 🟡 NÃO INICIADO

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
**Papel:** Backend | **Pontos:** 5 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Entidade RecurringRule + RecurringRuleRepository | Backend | PENDENTE |
| RecurringRuleService (criar, listar, desativar) | Backend | PENDENTE |
| Job agendado @Scheduled para gerar lançamentos na data certa | Backend | PENDENTE |
| Garantir idempotência (não duplicar se job rodar 2x no mesmo dia) | Backend | PENDENTE |
| Endpoints GET/POST/DELETE /api/recurring-rules | Backend | PENDENTE |
| Testes do job (geração na data, sem duplicar) | QA | PENDENTE |

---

### S-06-02 · Tela de recorrências
**Papel:** Frontend | **Pontos:** 3 | **Depende de:** S-06-01 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo de recorrências com lista | Frontend | PENDENTE |
| Formulário de criação (frequência, template do lançamento) | Frontend | PENDENTE |
| Ação de desativar recorrência | Frontend | PENDENTE |

---

### S-06-03 · API de acerto de contas
**Papel:** Backend | **Pontos:** 8 | **Depende de:** S-04-01 | **Status:** `PENDENTE`

> ⚠️ Esta é a story mais complexa do projeto. Requer atenção redobrada e revisão.

| Tarefa | Papel | Status |
|---|---|---|
| SettlementService.calculate(month) | Backend | PENDENTE |
| Agregar Installments do mês + lançamentos à vista | Backend | PENDENTE |
| Lógica FIFTY_FIFTY: split 50/50 de cada despesa | Backend | PENDENTE |
| Lógica PERSON_A / PERSON_B: 100% para a pessoa | Backend | PENDENTE |
| Lógica PROPORTIONAL: buscar receitas PERSON_A/B do mês | Backend | PENDENTE |
| PROPORTIONAL: calcular proporção (ex: 6000/10000 = 60%) | Backend | PENDENTE |
| PROPORTIONAL: mês sem receita → pending: true (não calcular) | Backend | PENDENTE |
| Resultado final: { debtor, creditor, amount } ou { settled: true } | Backend | PENDENTE |
| Endpoint GET /api/settlement?month=yyyy-MM | Backend | PENDENTE |
| Decidir comportamento de mês já acertado → memory/decisions/ | Arquiteto | PENDENTE |
| Teste: 50/50 simples | QA | PENDENTE |
| Teste: proporcional com receitas lançadas | QA | PENDENTE |
| Teste: proporcional sem receita individual → pending | QA | PENDENTE |
| Teste: só de uma pessoa (PERSON_A) | QA | PENDENTE |
| Teste: mês com parcelas de compras anteriores | QA | PENDENTE |
| Teste: acerto zerado (cada um pagou exatamente o que devia) | QA | PENDENTE |

---

### S-06-04 · Tela de acerto de contas
**Papel:** Frontend | **Pontos:** 5 | **Depende de:** S-06-03 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Módulo settlement com layout de acerto | Frontend | PENDENTE |
| Exibição de "X deve R$ Y a Z" com destaque visual | Frontend | PENDENTE |
| Alerta de proporção pendente ("Cadastre as receitas de [mês]") | Frontend | PENDENTE |
| Breakdown por pessoa: pagou vs. deveria pagar | Frontend | PENDENTE |
| Navegação por mês | Frontend | PENDENTE |
| Serviço HTTP SettlementService | Frontend | PENDENTE |

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
