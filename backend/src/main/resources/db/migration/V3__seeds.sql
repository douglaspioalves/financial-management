-- =============================================================================
-- V3__seeds.sql
-- Dados iniciais do sistema:
--   - 2 registros em person (os dois participantes do casal)
--   - 12 categorias cobrindo despesas, receitas e uso misto
--
-- IDs fixos (não gerados aleatoriamente) para garantir estabilidade em testes.
-- Idempotente: ON CONFLICT DO NOTHING permite reexecução sem erro.
-- Depende de: V2__initial_schema.sql (tabelas person e category já criadas)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Persons
-- Nomes neutros — o usuário configura os nomes reais no primeiro acesso.
-- Cor: azul (#4a7fc4) para Person A, coral (#e8927c) para Person B,
--       alinhados ao design system aprovado.
-- -----------------------------------------------------------------------------
INSERT INTO person (id, name, color, version)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'Person A', '#4a7fc4', 0),
    ('00000000-0000-0000-0000-000000000002', 'Person B', '#e8927c', 0)
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Categories
-- Tipo EXPENSE: despesas comuns do dia a dia.
-- Tipo INCOME:  receitas individuais (usadas no cálculo proporcional).
-- Tipo BOTH:    lançamentos que podem ser receita ou despesa conforme contexto.
--
-- Cores seguem o design system:
--   azul   #4a7fc4 — ações/navegação
--   coral  #e8927c — despesa/atenção
--   lilás  #b8a9d9 — destaque
--   verde  #7fc4a0 — receita/positivo
--   areia  #f0c080 — aviso
--   cinza  #c0c0c0 — neutro
-- -----------------------------------------------------------------------------
INSERT INTO category (id, name, type, color, version)
VALUES
    -- EXPENSE
    ('00000000-0000-0000-0000-000000000101', 'Alimentação',              'EXPENSE', '#e8927c', 0),
    ('00000000-0000-0000-0000-000000000102', 'Moradia',                  'EXPENSE', '#4a7fc4', 0),
    ('00000000-0000-0000-0000-000000000103', 'Transporte',               'EXPENSE', '#b8a9d9', 0),
    ('00000000-0000-0000-0000-000000000104', 'Saúde',                    'EXPENSE', '#7fc4a0', 0),
    ('00000000-0000-0000-0000-000000000105', 'Lazer',                    'EXPENSE', '#f0c080', 0),
    ('00000000-0000-0000-0000-000000000106', 'Vestuário',                'EXPENSE', '#e8927c', 0),
    ('00000000-0000-0000-0000-000000000107', 'Educação',                 'EXPENSE', '#4a7fc4', 0),
    ('00000000-0000-0000-0000-000000000108', 'Serviços e Assinaturas',   'EXPENSE', '#b8a9d9', 0),

    -- INCOME
    ('00000000-0000-0000-0000-000000000109', 'Salário',                  'INCOME',  '#7fc4a0', 0),
    ('00000000-0000-0000-0000-000000000110', 'Freelance',                'INCOME',  '#7fc4a0', 0),

    -- BOTH
    ('00000000-0000-0000-0000-000000000111', 'Transferência entre contas','BOTH',   '#b8a9d9', 0),
    ('00000000-0000-0000-0000-000000000112', 'Outros',                   'BOTH',    '#c0c0c0', 0)
ON CONFLICT (id) DO NOTHING;
