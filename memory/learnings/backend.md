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

### Sprint 07 (2026-05-30) — Exportacao CSV/XLSX

**Apache POI e BigDecimal:**
- Nunca converter `BigDecimal` para `double` ao escrever celulas XLSX — mesmo que a API do POI aceite `double`, isso viola a convencao do projeto e pode causar perda de precisao.
- Padrao correto: `cell.setCellValue(row.amount().toPlainString())` — o Excel reconhece como numero mesmo com o tipo `STRING` do POI quando formatado adequadamente. Alternativa: `CellType.NUMERIC` com `new BigDecimal(toPlainString())` via `double` apenas se escala for garantida.
- O revisor bloqueou o merge por essa violacao (REC-01). O commentario no codigo nao e suficiente para justificar desvio de convencao — use o padrao ou proponha uma excecao documentada no CLAUDE.md.

**Cobertura minima de testes para endpoints novos:**
- Todo endpoint novo deve ter pelo menos 3 testes de integracao: (1) sem token → 401, (2) parametro invalido → 400 com mensagem pt-br, (3) caso de sucesso → 200 com corpo valido.
- O revisor exigiu isso como REC-02 no Sprint 07 antes de aprovar o merge — adotar como regra padrao.

**Validacao de parametros de query:**
- Validar `format` e `month` como enum/YearMonth antes de qualquer processamento — rejeitar com 400 e mensagem pt-br. Nunca passar strings brutas da URL para headers HTTP (Content-Disposition) ou para consultas SQL.

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
