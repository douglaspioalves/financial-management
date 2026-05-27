# Learnings — DevOps
> Atualizado pelo agente devops após cada retrospectiva.

## Ambiente local (Windows)

- Ferramentas necessárias: JDK 21, Node 20+, Angular CLI, Docker Desktop, Git.
- Portas: frontend 4200, backend 8080, Postgres 5432.
- Comando único: `docker compose up --build`
- README mantido em pt-br com instruções para Windows (PowerShell).

## Convenções de commit e tag

- Mensagens de commit em pt-br, descritivas.
- Tag ao fim de cada fatia/sprint aprovado: `fatia-N` ou `sprint-NN`.
- `.env.example` mantido; nunca commitar `.env` com segredos reais.

## Variáveis de ambiente mínimas

```
DB_URL=jdbc:postgresql://postgres:5432/gastos
DB_USER=gastos
DB_PASS=gastos
JWT_SECRET=<gerar string longa aleatória>
JWT_EXPIRATION_MS=86400000
```

## Atenção

- `docker compose up` deve partir de banco zerado e rodar todas as migrations limpas.
- Flyway roda automaticamente no start do backend — não rodar scripts SQL manuais.

## Aprendizados das retrospectivas

> (preenchido ao longo dos sprints)
