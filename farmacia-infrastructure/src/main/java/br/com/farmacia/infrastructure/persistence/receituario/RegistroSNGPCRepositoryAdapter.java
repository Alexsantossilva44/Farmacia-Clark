package br.com.farmacia.infrastructure.persistence.receituario;

import br.com.farmacia.domain.receituario.entity.RegistroSNGPC;
import br.com.farmacia.domain.receituario.repository.RegistroSNGPCRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link RegistroSNGPCRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class RegistroSNGPCRepositoryAdapter implements RegistroSNGPCRepository {

    private final RegistroSNGPCJpaRepository jpaRepository;

    @Override
    @Transactional
    public RegistroSNGPC save(RegistroSNGPC registro) {
        if (registro.getId() == null) {
            registro.atribuirId(UUID.randomUUID());
        }
        RegistroSNGPCJpaEntity salvo =
            jpaRepository.save(RegistroSNGPCPersistenceMapper.toJpa(registro));
        return RegistroSNGPCPersistenceMapper.toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RegistroSNGPC> findById(UUID id) {
        return jpaRepository.findById(id).map(RegistroSNGPCPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroSNGPC> findPendentesParaReprocessamento(LocalDateTime limite, int maxTentativas) {
        return jpaRepository.findPendentesParaReprocessamento(limite, maxTentativas).stream()
            .map(RegistroSNGPCPersistenceMapper::toDomain)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RegistroSNGPC getReferenceById(UUID id) {
        return RegistroSNGPC.builder().id(id).build();
    }
}
