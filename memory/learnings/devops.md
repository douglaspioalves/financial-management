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

### Sprint 06 (2026-05-30)

- Merge de 6 branches pode gerar conflito "both added" em arquivos de checkpoint criados por dois agentes independentes. Resolver manualmente preservando o conteúdo de ambos.
- Servidor git local rejeita `git push --tags` com HTTP 403 — tags criadas localmente são suficientes para rastreabilidade enquanto `master` está sincronizado.
- `annotationProcessorPaths` para Lombok deve estar no `pom.xml` para garantir que `mvn test` funcione no ambiente remoto (sem Maven Wrapper configurado).
- O arquivo `.mvn/wrapper/maven-wrapper.properties` está no `.gitignore` por padrão. Para commitar, usar `git add -f`.

### Sprint 01 (2026-05-28)

- JWT_SECRET padrão no docker-compose deve ser Base64 válido (≥ 256 bits) ou o JwtService precisará de fallback UTF-8.
  Valor seguro de exemplo: `Z2VzdG9yLWRlLWdhc3Rvcy1qd3Qtc2VjcmV0LWRldi0yNTZi`
- Validar `docker compose up --build` com banco zerado antes de fechar o sprint — migrations podem quebrar silenciosamente.
- CI com H2 em memória para testes do backend: adicionar `spring.profiles.active=test` e desabilitar Flyway no perfil de teste.
