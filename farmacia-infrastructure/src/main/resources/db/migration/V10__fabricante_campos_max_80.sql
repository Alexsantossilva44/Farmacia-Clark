-- Reduz limite de razao_social e nome_fantasia de 150 → 80 caracteres
ALTER TABLE fabricantes
    ALTER COLUMN razao_social TYPE VARCHAR(80),
    ALTER COLUMN nome_fantasia TYPE VARCHAR(80);