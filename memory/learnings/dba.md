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

### Sprint 01 (2026-05-27)

**Decisões de schema tomadas:**
- Tabela `users` (não `user`) — `user` e `USER` são palavras reservadas no PostgreSQL; usar sem aspas duplas causa erros silenciosos em alguns contextos
- `chk_transaction_card_credit` como CHECK constraint no banco garante a regra `card_id IS NOT NULL ↔ payment_method = 'CREDIT'` independente da aplicação
- `EXTRACT(DAY FROM reference_month) = 1` como CHECK constraint garante invariante de "sempre dia 1 do mês" no nível do banco
- `UNIQUE(transaction_id, number)` em `installment` — evita parcelas duplicadas por bug no serviço de geração
- Índice parcial `WHERE active = true` em `recurring_rule.next_date` — otimiza o job que processa apenas regras ativas

**Seeds:**
- UUIDs fixos (`00000000-0000-0000-0000-000000000001`) para persons e categorias — estabilidade em testes de integração e facilidade de debug nos logs
- `ON CONFLICT (id) DO NOTHING` — migrations idempotentes; seguro rodar `docker compose up` sem `--force-recreate`
- Nomes neutros "Person A" / "Person B" nas seeds — usuário configura os nomes reais no primeiro acesso (não forçar "João/Maria")
