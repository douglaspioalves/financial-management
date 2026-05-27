# /memory — Memória do Projeto

Repositório de conhecimento vivo do projeto Gestor de Gastos.
Tudo que os agentes aprendem, decidem e registram fica aqui.
**Nunca delete arquivos desta pasta — é o histórico do projeto.**

## Estrutura

```
memory/
├── README.md              ← este arquivo
├── sprints/               ← backlog e progresso de cada sprint
│   ├── sprint-01.md
│   ├── sprint-02.md
│   └── ...
├── plannings/             ← atas de sprint planning
│   ├── planning-sprint-01.md
│   └── ...
├── dailies/               ← registros diários (antes + depois)
│   ├── YYYY-MM-DD.md
│   └── ...
├── reviews/               ← atas de sprint review
│   ├── review-sprint-01.md
│   └── ...
├── retros/                ← atas de retrospectiva
│   ├── retro-sprint-01.md
│   └── ...
├── decisions/             ← decisões técnicas e de produto tomadas no processo
│   └── YYYY-MM-DD-titulo.md
└── learnings/             ← aprendizados consolidados por área
    ├── backend.md
    ├── frontend.md
    ├── dba.md
    ├── devops.md
    └── negocio.md
```

## Convenções de nomenclatura

- Sprints: `sprint-NN.md` (ex.: `sprint-01.md`)
- Plannings: `planning-sprint-NN.md`
- Dailies: `YYYY-MM-DD.md` (ex.: `2026-06-02.md`)
- Reviews: `review-sprint-NN.md`
- Retros: `retro-sprint-NN.md`
- Decisões: `YYYY-MM-DD-titulo-curto.md` (ex.: `2026-06-05-calculo-proporcional.md`)

## Quem atualiza o quê

| Arquivo | Agente responsável |
|---|---|
| `sprints/sprint-NN.md` | Scrum Master (cria no planning; atualiza status diariamente) |
| `plannings/` | Scrum Master |
| `dailies/YYYY-MM-DD.md` | Todos os agentes ativos no dia |
| `reviews/` | Scrum Master + todos os agentes |
| `retros/` | Scrum Master (facilita); todos contribuem |
| `decisions/` | Arquiteto ou agente que tomou a decisão |
| `learnings/` | Cada agente atualiza o seu arquivo após retros |

## Regra de leitura para os agentes

**Ao iniciar qualquer sessão**, o agente deve:
1. Ler `memory/sprints/sprint-NN.md` do sprint atual.
2. Ler o último `memory/dailies/YYYY-MM-DD.md` disponível.
3. Ler `memory/learnings/<sua-area>.md`.
Isso garante continuidade mesmo sem memória entre sessões.
