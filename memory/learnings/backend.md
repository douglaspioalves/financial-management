# Learnings — Backend
> Atualizado pelo agente backend após cada retrospectiva.
> Leia no início de cada sessão para não repetir erros.

## Regras de negócio críticas (nunca esquecer)

- Parcelamento: gera N `Installment`; última parcela absorve diferença de centavos.
- Acerto usa `Installment.reference_month`, não a data da compra.
- Proporcional: só receitas com `split_rule=PERSON_A/B` entram no cálculo.
  Mês sem receita individual → pendência, nunca 50/50 silencioso.
- Dinheiro: sempre `BigDecimal`. Nunca `double`/`float`.
- Concorrência: `@Version` em toda entidade editável.

## Padrões estabelecidos

- Camadas: controller → service → repository → domain. DTOs na fronteira.
- Mensagens de validação/erro em pt-br (expostas ao usuário).
- Migrations Flyway versionadas; nunca `ddl-auto: update` além de dev.
- Testes obrigatórios para: geração de parcelas, cálculo de acerto, proporção.

## Aprendizados das retrospectivas

### Sprint 01 (2026-05-27)

**Spring Security 6 + JWT (JJWT 0.12.x):**
- API do JJWT mudou da 0.11 para 0.12: usar `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` (não `parserBuilder`)
- `DaoAuthenticationProvider` deve ser declarado como `@Bean` explícito quando se usa `UserDetailsService` + `PasswordEncoder` customizados — evita o aviso "multiple UserDetailsService beans"
- `JwtAuthenticationFilter` deve capturar exceções do parse do token silenciosamente e deixar a request prosseguir; o `SecurityFilterChain` nega o acesso depois
- `User` implementar `UserDetails` diretamente é o padrão mais limpo para esse projeto (sem camada wrapper)
- Tabela `users` (plural) — `user` é palavra reservada no PostgreSQL e causa erros silenciosos

**GlobalExceptionHandler:**
- Separar `MethodArgumentNotValidException` (400 por campo) de `IllegalArgumentException` (409) é o padrão correto para esse projeto
- Retornar `Map<String, String>` com chave `mensagem` para erros de negócio e mapa de campos para erros de validação
