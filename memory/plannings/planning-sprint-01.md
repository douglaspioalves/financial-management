# Planning — Sprint 01
**Data:** 2026-05-26
**Sprint:** 01 de 07
**Período:** 2026-05-26 → 2026-06-06 (10 dias úteis)
**Fatia do roadmap:** 0 e 1 — Fundação e Autenticação
**Conduzido por:** Scrum Master

---

## Objetivo do sprint

Ter o app rodando de ponta a ponta: Docker Compose sobe Postgres + backend + frontend,
e é possível cadastrar e fazer login com JWT — base para todo o desenvolvimento seguinte.

---

## Stories selecionadas

| ID | Story | Papel | Pontos | Depende de |
|---|---|---|---|---|
| S-01-01 | Esqueleto do monorepo e Docker Compose | DevOps | 3 | — |
| S-01-02 | Migration inicial do banco | DBA | 2 | S-01-01 |
| S-01-03 | Cadastro e login com JWT | Backend | 5 | S-01-02 |
| S-01-04 | Telas de login e cadastro | Frontend | 4 | S-01-03 |

**Total de pontos:** 14

## Tarefas paralelas identificadas

- **Paralelo A (dia 1–2, início imediato):** S-01-01 DevOps — esqueleto e Docker.
- **Paralelo B (dia 2–4, após esqueleto):** S-01-02 DBA — migrations; DevOps finaliza README.
- **Paralelo C (dia 3–7, após migrations):** S-01-03 Backend — autenticação; Frontend começa tema Angular Material.
- **Sequencial (dia 5–9):** S-01-04 Frontend depende do backend estar funcional.
- **Fechamento (dia 9–10):** QA testa; Revisor valida segurança; DevOps comita.

## Riscos e pontos de atenção

- Configuração do Spring Security costuma ser a parte mais trabalhosa da Fatia 1.
  Reserve tempo extra para o filtro JWT.
- Validar que o Docker Compose sobe num banco zerado antes de avançar para S-01-02.
- O tema do Angular Material precisa bater com o design system aprovado — fazer cedo
  para não retrabalhar nas telas.

## Definição de pronto (DoD) do sprint

- [ ] `docker compose up --build` sobe tudo sem erro
- [ ] Migrations aplicadas; seeds presentes no banco
- [ ] Register/login funcionando; 401 em rotas protegidas
- [ ] Telas de login e cadastro no browser com modo claro/escuro
- [ ] Testes passando: `./mvnw test`
- [ ] Review registrada em `memory/reviews/review-sprint-01.md`
- [ ] Retro registrada em `memory/retros/retro-sprint-01.md`
- [ ] Learnings atualizados em `memory/learnings/`
