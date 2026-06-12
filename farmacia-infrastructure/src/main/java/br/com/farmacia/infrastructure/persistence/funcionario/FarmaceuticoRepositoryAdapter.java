package br.com.farmacia.infrastructure.persistence.funcionario;

import br.com.farmacia.domain.funcionario.entity.Farmaceutico;
import br.com.farmacia.domain.funcionario.repository.FarmaceuticoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link FarmaceuticoRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class FarmaceuticoRepositoryAdapter implements FarmaceuticoRepository {

    private final FarmaceuticoJpaRepository jpaRepository;

    @Override
    @Transactional
    public Farmaceutico save(Farmaceutico farmaceutico) {
        if (farmaceutico.getId() == null) {
            farmaceutico.atribuirId(UUID.randomUUID());
        }
        return FarmaceuticoPersistenceMapper.toDomain(
            jpaRepository.save(FarmaceuticoPersistenceMapper.toJpa(farmaceutico)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Farmaceutico> findById(UUID id) {
        return jpaRepository.findById(id).map(FarmaceuticoPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Farmaceutico> findByFuncionarioId(UUID funcionarioId) {
        return jpaRepository.findByFuncionarioId(funcionarioId)
            .map(FarmaceuticoPersistenceMapper::toDomain);
    }
}
