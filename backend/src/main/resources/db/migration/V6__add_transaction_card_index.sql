-- =============================================================================
-- V6__add_transaction_card_index.sql
-- Gestor de Gastos — Índice em transaction.card_id
--
-- Motivação: o CardService verifica se um cartão possui transações antes de
-- permitir a exclusão (SELECT ... WHERE card_id = ?). Sem este índice, a
-- query realiza full scan em toda a tabela transaction.
--
-- Sprint 03 — S-03-00
-- =============================================================================

CREATE INDEX idx_transaction_card_id ON transaction (card_id);
