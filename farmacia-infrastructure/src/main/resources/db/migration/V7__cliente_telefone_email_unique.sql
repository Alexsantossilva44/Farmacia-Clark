-- ============================================================
-- V7 — Unicidade de telefone e e-mail em clientes
-- ============================================================
-- Garantia no banco contra race condition (TOCTOU) entre validar*Unico e save.
-- Índices parciais: múltiplos NULL continuam permitidos (legado V3).
--
-- Pré-limpeza: em dev/testes podem existir duplicatas (ex.: 21911112222).
-- Mantém o cliente mais antigo (data_cadastro, id) e anula telefone/e-mail nos demais.

WITH telefone_duplicado AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY telefone
               ORDER BY data_cadastro ASC, id ASC
           ) AS rn
    FROM clientes
    WHERE telefone IS NOT NULL AND telefone <> ''
)
UPDATE clientes c
SET telefone = NULL
FROM telefone_duplicado d
WHERE c.id = d.id
  AND d.rn > 1;

WITH email_duplicado AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY LOWER(email)
               ORDER BY data_cadastro ASC, id ASC
           ) AS rn
    FROM clientes
    WHERE email IS NOT NULL AND email <> ''
)
UPDATE clientes c
SET email = NULL
FROM email_duplicado d
WHERE c.id = d.id
  AND d.rn > 1;

CREATE UNIQUE INDEX uk_clientes_telefone
    ON clientes (telefone)
    WHERE telefone IS NOT NULL AND telefone <> '';

CREATE UNIQUE INDEX uk_clientes_email
    ON clientes (email)
    WHERE email IS NOT NULL AND email <> '';
