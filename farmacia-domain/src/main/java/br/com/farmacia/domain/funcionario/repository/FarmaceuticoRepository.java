package br.com.farmacia.domain.funcionario.repository;

import br.com.farmacia.domain.funcionario.entity.Farmaceutico;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para farmacêuticos.
 *
 * @author Alex Silva e Claude
 */
public interface FarmaceuticoRepository {
    Farmaceutico save(Farmaceutico farmaceutico);
    Optional<Farmaceutico> findById(UUID id);
    Optional<Farmaceutico> findByFuncionarioId(UUID funcionarioId);
}
