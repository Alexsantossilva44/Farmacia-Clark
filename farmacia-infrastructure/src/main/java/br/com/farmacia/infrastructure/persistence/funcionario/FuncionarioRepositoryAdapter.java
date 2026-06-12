package br.com.farmacia.infrastructure.persistence.funcionario;

import br.com.farmacia.domain.funcionario.entity.Cargo;
import br.com.farmacia.domain.funcionario.entity.Funcionario;
import br.com.farmacia.domain.funcionario.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link FuncionarioRepository}.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class FuncionarioRepositoryAdapter implements FuncionarioRepository {

    private final FuncionarioJpaRepository jpaRepository;
    private final CargoJpaRepository cargoJpaRepository;

    @Override
    @Transactional
    public Funcionario save(Funcionario funcionario) {
        if (funcionario.getId() == null) {
            funcionario.atribuirId(UUID.randomUUID());
        }
        FuncionarioJpaEntity salvo =
            jpaRepository.save(FuncionarioPersistenceMapper.toJpa(funcionario));
        return toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Funcionario> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Funcionario> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    private Funcionario toDomain(FuncionarioJpaEntity e) {
        Cargo cargo = e.getCargoId() == null ? null
            : cargoJpaRepository.findById(e.getCargoId())
                .map(CargoPersistenceMapper::toDomain)
                .orElse(Cargo.builder().id(e.getCargoId()).build());
        return FuncionarioPersistenceMapper.toDomain(e, cargo);
    }
}
