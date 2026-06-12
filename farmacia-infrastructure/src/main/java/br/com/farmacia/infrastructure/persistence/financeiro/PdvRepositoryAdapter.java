package br.com.farmacia.infrastructure.persistence.financeiro;

import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.financeiro.repository.PdvRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link PdvRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class PdvRepositoryAdapter implements PdvRepository {

    private final PdvJpaRepository jpaRepository;

    @Override
    @Transactional
    public PDV save(PDV pdv) {
        if (pdv.getId() == null) {
            pdv.atribuirId(UUID.randomUUID());
        }
        return PdvPersistenceMapper.toDomain(jpaRepository.save(PdvPersistenceMapper.toJpa(pdv)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PDV> findById(UUID id) {
        return jpaRepository.findById(id).map(PdvPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PDV> findByNumero(String numero) {
        return jpaRepository.findByNumero(numero).map(PdvPersistenceMapper::toDomain);
    }
}
