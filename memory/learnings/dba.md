# Learnings — DBA
> Atualizado pelo agente banco após cada retrospectiva.

## Convenções estabelecidas

- Tabelas e colunas em inglês, snake_case.
- Dinheiro: `NUMERIC(12,2)`. Datas: `DATE`/`TIMESTAMP`.
- Toda entidade editável pelos dois logins tem coluna `version` (optimistic locking).
- Chaves primárias UUID em todas as tabelas.
- `Installment.transaction_id` com `ON DELETE CASCADE`.

## Índices obrigatórios

- `transaction.date` (filtro de período)
- `installment.reference_month` (acerto mensal)
- `transaction.paid_by_person_id` (agrupamento por pessoa)
- `transaction.category_id` (relatórios por categoria)

## Seeds iniciais (migration de setup)

- 2 registros em `person` (os dois participantes).
- Categorias iniciais: Mercado, Transporte, Saúde, Lazer, Moradia, Contas, Restaurante,
  Educação, Roupas, Salário, Freelance, Outros.

## Atenção

- `card_id` só faz sentido quando `payment_method = CREDIT` — validar na aplicação.
- Migrations são **imutáveis** depois de aplicadas. Nova mudança = nova migration.

## Aprendizados das retrospectivas

> (preenchido ao longo dos sprints)
