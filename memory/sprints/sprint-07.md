# Sprint 07
**Período:** Semanas 13–14
**Epic:** Exportação e Qualidade Final
**Fatia:** 7
**Objetivo:** Exportação de dados, polimento de UX, testes de ponta a ponta e release v1.0.0.
**Status:** CONCLUIDO — 129 testes, 0 falhas, BUILD SUCCESS. Retro final registrada em 2026-05-30.

---

## Tarefas paralelas — início do sprint

```
DIA 1–4 (paralelo)
├── Backend:  S-07-01a — Export service (xlsx/csv)
├── Frontend: S-07-03 — Polimento de UX e responsividade
└── QA:       S-07-02 — Testes de ponta a ponta

DIA 4–6 (após backend export)
└── Frontend: S-07-01b — Botão de exportar na tela de lançamentos

DIA 5–8 (paralelo)
├── Revisor:  Revisão de segurança completa
└── QA:       Teste de carga leve (1 ano de dados)

DIA 8–9 (convergência)
└── Arquiteto + DevOps: Documentação final + api.md completo

DIA 10 (release)
└── DevOps:   Tag v1.0.0 + README final + memory atualizada
```

---

## Backlog do sprint

### S-07-01 · Exportação Excel/CSV
**Papel:** Backend + Frontend | **Pontos:** 4 | **Status:** `PENDENTE`

| Tarefa | Papel | Status |
|---|---|---|
| Adicionar dependência Apache POI (xlsx) ao pom.xml | Backend | CONCLUIDO |
| ExportService (gerar xlsx e csv a partir de transactions do mês) | Backend | CONCLUIDO |
| Colunas: data, descrição, categoria, quem pagou, valor, divisão, parcela | Backend | CONCLUIDO |
| Endpoint GET /api/export?month=yyyy-MM&format=csv|xlsx | Backend | CONCLUIDO |
| Botão "Exportar" na tela de lançamentos | Frontend | CONCLUIDO |
| Download do arquivo no browser | Frontend | CONCLUIDO |

---

### S-07-02 · Testes de ponta a ponta e revisão de segurança
**Papel:** QA + Revisor | **Pontos:** 5 | **Status:** `CONCLUIDO (parcial)`

| Tarefa | Papel | Status |
|---|---|---|
| Revisar autenticação de todos os endpoints | Revisor | CONCLUIDO — revisão em memory/reviews/review-sprint-07.md |
| Verificar que nenhum dado sensível (password_hash) vaza em respostas | Revisor | CONCLUIDO — checklist 9 pontos aprovado |
| Teste de fluxo completo: login → lançamento parcelado → dashboard → acerto | QA | CONCLUIDO — TC-F01 a TC-F11 |
| Teste de carga leve: 1 ano de dados (~200 lançamentos) | QA | PENDENTE — nao implementado |
| Teste de modo escuro em todas as telas | QA | PENDENTE — nao implementado |
| Relatório de revisão em memory/reviews/ | Revisor | CONCLUIDO — review-sprint-07.md registrado |

---

### S-07-03 · Polimento de UX e responsividade
**Papel:** Frontend | **Pontos:** 4 | **Status:** `CONCLUIDO (parcial)`

| Tarefa | Papel | Status |
|---|---|---|
| Auditar responsividade em todas as telas (viewport mobile) | Frontend | CONCLUIDO |
| Implementar estados vazios com mensagens amigáveis em pt-br | Frontend | CONCLUIDO |
| Loading/skeleton em listas e cards de dashboard | Frontend | CONCLUIDO |
| Revisão final do modo escuro em todas as telas | Frontend | PENDENTE — backlog pos-sprint |
| Tratar erros de API com toasts/snackbar em pt-br | Frontend | PENDENTE — backlog pos-sprint |

---

### S-07-04 · Documentação final e release
**Papel:** DevOps + Arquiteto | **Pontos:** 2 | **Status:** `CONCLUIDO (parcial)`

| Tarefa | Papel | Status |
|---|---|---|
| Atualizar README com estado final | DevOps | CONCLUIDO — export e responsividade adicionados |
| Completar docs/api.md com todos os endpoints | Arquiteto | CONCLUIDO — Fatias 6a, 6b e 7 documentadas |
| Revisão final de memory/ (learnings, decisions) | Scrum Master | CONCLUIDO — learnings backend/devops atualizados; retro final registrada |
| Criar tag v1.0.0 no git | DevOps | CONCLUIDO — tag v1.0.0 criada localmente |
| Registrar retrospectiva final em memory/retros/ | Scrum Master | CONCLUIDO — retro-sprint-07.md registrado em 2026-05-30 |

---

## Progresso diário

| Dia | Data | Stories avançadas | Impedimentos |
|---|---|---|---|
| 1 | 2026-05-30 | S-07-01 (backend+frontend export), S-07-02 (FlowIntegrationTest), S-07-03 (UX/responsividade) | — |
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

## Definition of Done — Sprint 07 e v1.0.0

- [x] Export xlsx e csv funcionando no browser
- [x] Nenhum endpoint público sem autenticação — checklist de segurança revisado (9/9 pontos PASS ou N/A)
- [x] Fluxo completo testado ponta a ponta — TC-F01 a TC-F11
- [x] App responsivo no celular
- [ ] Modo escuro revisado em todas as telas — backlog pos-sprint
- [x] States vazios e loading implementados
- [x] README e api.md atualizados — docs/api.md completo (Fatias 6a, 6b, 7); README v1.0.0
- [x] memory/ atualizada — learnings backend/devops/processo registrados; retro e review registrados
- [x] Todos os testes passando: `./mvnw test` (BUILD SUCCESS — 129 testes, 0 falhas, 0 skipped)
- [ ] `docker compose up --build` limpo — pendente: Docker daemon nao disponivel no sandbox; validar manualmente
- [x] Tag `v1.0.0` criada no git (local) — push aguarda validação manual com docker compose
- [x] Review registrada em `memory/reviews/review-sprint-07.md`
- [x] Retro FINAL registrada em `memory/retros/retro-sprint-07.md`
- [x] Learnings finais atualizados em `memory/learnings/` (backend.md, devops.md)
