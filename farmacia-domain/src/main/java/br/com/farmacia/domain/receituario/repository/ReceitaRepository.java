package br.com.farmacia.domain.receituario.repository;

import br.com.farmacia.domain.receituario.entity.Receita;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para persistência de receitas.
 *
 * @author Alex Silva e Claude
 */
public interface ReceitaRepository {
    Receita save(Receita receita);
    Optional<Receita> findById(UUID id);
    Optional<Receita> findByNumeroReceita(String numero);
    Receita getReferenceById(UUID id);
}
