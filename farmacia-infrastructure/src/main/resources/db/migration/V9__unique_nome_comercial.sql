-- Impede dois medicamentos ATIVOS com o mesmo nome comercial (case-insensitive).
-- Medicamentos inativos (ativo = false) ficam fora do índice — exclusão lógica
-- não impede futuro re-cadastro com o mesmo nome.
CREATE UNIQUE INDEX uq_medicamentos_nome_comercial_ativo
    ON medicamentos(lower(nome_comercial))
    WHERE ativo = true;
