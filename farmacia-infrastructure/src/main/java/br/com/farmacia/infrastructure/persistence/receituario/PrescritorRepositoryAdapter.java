package br.com.farmacia.infrastructure.persistence.receituario;

import br.com.farmacia.domain.receituario.entity.Prescritor;
import br.com.farmacia.domain.receituario.repository.PrescritorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída para {@link PrescritorRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class PrescritorRepositoryAdapter implements PrescritorRepository {

    private final PrescritorJpaRepository jpaRepository;

    @Override
    @Transactional
    public Prescritor save(Prescritor prescritor) {
        if (prescritor.getId() == null) {
            prescritor.atribuirId(UUID.randomUUID());
        }
        return PrescritorPersistenceMapper.toDomain(
            jpaRepository.save(PrescritorPersistenceMapper.toJpa(prescritor)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prescritor> findById(UUID id) {
        return jpaRepository.findById(id).map(PrescritorPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prescritor> findByCrmAndUfCrm(String crm, String ufCrm) {
        return jpaRepository.findByCrmAndUfCrm(crm, ufCrm).map(PrescritorPersistenceMapper::toDomain);
    }
}
