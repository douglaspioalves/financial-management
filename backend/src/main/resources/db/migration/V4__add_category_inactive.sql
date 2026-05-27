-- =============================================================================
-- V4__add_category_inactive.sql
-- Adiciona suporte a soft delete na tabela category.
--
-- Decisão de produto: ao invés de deleção física (que quebraria FKs em
-- transaction, budget e recurring_rule), categorias são marcadas como inativas.
-- Elas deixam de aparecer nos formulários mas lançamentos históricos mantêm
-- a referência intacta, preservando relatórios e acertos de contas passados.
--
-- Ver: memory/decisions/2026-05-27-delete-category.md
--
-- Banco: PostgreSQL 16
-- Sprint: 02
-- =============================================================================

-- Adiciona coluna inactive. DEFAULT false garante que todos os registros
-- existentes permaneçam ativos automaticamente, sem necessidade de UPDATE.
ALTER TABLE category
    ADD COLUMN inactive BOOLEAN NOT NULL DEFAULT false;

-- Índice parcial para otimizar a query mais frequente: buscar apenas categorias
-- ativas (WHERE inactive = false). Como a grande maioria das categorias estará
-- ativa, o índice parcial é menor e mais eficiente que um índice total.
CREATE INDEX idx_category_active ON category (id) WHERE inactive = false;
