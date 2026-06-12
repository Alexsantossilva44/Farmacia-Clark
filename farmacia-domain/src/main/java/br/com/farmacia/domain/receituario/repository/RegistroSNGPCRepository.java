package br.com.farmacia.domain.receituario.repository;

import br.com.farmacia.domain.receituario.entity.RegistroSNGPC;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para o registro SNGPC.
 *
 * @author Alex Silva e Claude
 */
public interface RegistroSNGPCRepository {
    RegistroSNGPC save(RegistroSNGPC registro);
    Optional<RegistroSNGPC> findById(UUID id);
    List<RegistroSNGPC> findPendentesParaReprocessamento(LocalDateTime limite, int maxTentativas);
    RegistroSNGPC getReferenceById(UUID id);
}
