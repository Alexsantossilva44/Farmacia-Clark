package br.com.farmacia.domain.receituario.repository;

import br.com.farmacia.domain.receituario.entity.Prescritor;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para prescritores.
 *
 * @author Alex Silva e Claude
 */
public interface PrescritorRepository {
    Prescritor save(Prescritor prescritor);
    Optional<Prescritor> findById(UUID id);
    Optional<Prescritor> findByCrmAndUfCrm(String crm, String ufCrm);
}
