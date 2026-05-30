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

### Sprint 07 (2026-05-30) — Merges finais e testes bloqueados

**Branches de infra/chore devem ser separadas de branches de feature:**
- Commits de `.gitignore`, remocao de `@Disabled`, atualizacao de dependencias de CI devem ir em uma branch `chore/sprint-NN`, nunca misturados em `feature/sNN-backend` ou `feature/sNN-frontend`.
- No Sprint 07 commits de infra em branches erradas geraram reverts e poluiram o historico. Exemplo do problema: `chore: ignorar diretorio de worktrees` commitado duas vezes em branches diferentes.

**Testes com @Disabled devem ser rastreados ativamente:**
- Ao encontrar um `@Disabled` em qualquer teste, registrar no `sprint-NN.md` com a razao e o criterio de remocao.
- Nao deixar para o DevOps final remover — qualquer agente pode e deve remover quando o impedimento for resolvido.
- No Sprint 07, `BudgetIntegrationTest` e `DashboardIntegrationTest` ficaram desabilitados por todo o sprint e foram removidos apenas na branch `fix/enable-disabled-integration-tests` pelo ultimo DevOps.

**Procedimento de merge na ordem correta:**
1. Schema/migrations primeiro (se houver).
2. Backend e Frontend em paralelo.
3. Testes.
4. Fixes pos-revisao.
5. Chores/infra.
- No Sprint 07 a ordem foi respeitada e nao houve conflito — manter esse padrao.

**Docker nao disponivel no sandbox — documentar desde o inicio:**
- Se o Docker daemon nao estiver disponivel, registrar como impedimento no `sprint-NN.md` no Dia 1, nao como pendencia silenciosa no DoD.
- Criar uma secao "Validacao pendente fora do sandbox" para que o time saiba o que falta validar manualmente.

### Sprint 01 (2026-05-28)

- JWT_SECRET padrão no docker-compose deve ser Base64 válido (≥ 256 bits) ou o JwtService precisará de fallback UTF-8.
  Valor seguro de exemplo: `Z2VzdG9yLWRlLWdhc3Rvcy1qd3Qtc2VjcmV0LWRldi0yNTZi`
- Validar `docker compose up --build` com banco zerado antes de fechar o sprint — migrations podem quebrar silenciosamente.
- CI com H2 em memória para testes do backend: adicionar `spring.profiles.active=test` e desabilitar Flyway no perfil de teste.
