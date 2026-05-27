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

> (preenchido ao longo dos sprints)
