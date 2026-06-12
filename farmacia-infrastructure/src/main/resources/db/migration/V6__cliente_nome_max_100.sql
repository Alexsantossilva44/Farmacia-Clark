-- ============================================================
-- V6 — Limite do nome do cliente (150 → 100 caracteres)
-- ============================================================
-- Motivo: regra de negócio alinhada ao front (maxLength=100) e à API (@Size max=100).
-- Pré-requisito: nenhum registro em clientes.nome com mais de 100 caracteres.
ALTER TABLE clientes
    ALTER COLUMN nome TYPE VARCHAR(100);
