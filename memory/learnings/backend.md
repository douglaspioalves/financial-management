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

### Sprint 06 (2026-05-30)

**SettlementService — armadilhas:**
- `personRepository.findAll()` retorna lista imutável em contexto de mock — chamar `.sort()` direto lança `UnsupportedOperationException`. Sempre envolver em `new ArrayList<>()` antes de ordenar.
- Agentes paralelos podem commitar código na branch errada (ex.: SettlementController na branch de recorrência). Revisão pós-merge obrigatória.
- Testar o acerto de contas exige meses distintos por cenário para evitar interferência entre testes (TC-IT-04 usa 2026-06, TC-IT-05 usa 2026-07, TC-IT-06 usa 2026-09).
- `findIndividualIncomesByMonth` foi adicionado duas vezes por agentes diferentes — gera erro de compilação "method already defined". Usar `grep` para verificar duplicatas após merge de múltiplas branches que tocam o mesmo repositório.

**RecurringRuleService:**
- Validação `@NotBlank` obrigatória em `description` pois o método de idempotência `existsByDescriptionAndAmountAndDateAndCategoryId` usa description como parte da chave — null/vazio gera falso positivo silencioso.

**Lombok + Maven (ambiente remoto):**
- No ambiente de CI remoto (sem `.mvn/wrapper/`), o `mvn` do sistema não executa Lombok via annotation processing automático com Java 21. Solução: adicionar `annotationProcessorPaths` explícito no `maven-compiler-plugin`.
- Arquivo `.mvn/wrapper/maven-wrapper.properties` pode estar ausente após worktrees serem limpas. Recriar aponta para Maven 3.9.11.

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
