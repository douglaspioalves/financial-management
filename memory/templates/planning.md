# Planning — Sprint NN
**Data:** YYYY-MM-DD
**Sprint:** NN de 07
**Período:** YYYY-MM-DD → YYYY-MM-DD
**Fatia do roadmap:** N — [nome da fatia]
**Conduzido por:** Scrum Master

---

## Objetivo do sprint

> Uma frase clara: o que estará funcionando ao fim deste sprint que não funcionava antes.

---

## Stories selecionadas

| ID | Story | Papel | Pontos | Depende de |
|---|---|---|---|---|
| S-NN-01 | [título] | backend | 3 | — |
| S-NN-02 | [título] | dba | 2 | — |
| S-NN-03 | [título] | frontend | 3 | S-NN-01 |
| S-NN-04 | [título] | devops | 1 | — |

## Tarefas paralelas identificadas

> Liste quais tarefas podem correr em paralelo (sem dependência entre si).

- **Paralelo A (início imediato):** S-NN-02 (dba) + S-NN-04 (devops)
- **Paralelo B (após paralelo A):** S-NN-01 (backend) — depende de migrations
- **Sequencial:** S-NN-03 (frontend) — depende do backend

## Riscos e pontos de atenção

- [risco identificado e estratégia de mitigação]

## Definição de pronto (DoD) do sprint

- [ ] Todas as stories com status CONCLUÍDO
- [ ] Testes passando (`./mvnw test`)
- [ ] Docker Compose sobe sem erro (`docker compose up --build`)
- [ ] Review registrada em `memory/reviews/`
- [ ] Retro registrada em `memory/retros/`
- [ ] Learnings atualizados em `memory/learnings/`
