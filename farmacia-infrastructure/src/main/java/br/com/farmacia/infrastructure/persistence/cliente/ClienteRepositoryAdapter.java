package br.com.farmacia.infrastructure.persistence.cliente;

import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta {@link ClienteRepository}
 * sobre o Spring Data JPA.
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClienteJpaRepository jpaRepository;

    @Override
    @Transactional
    public Cliente save(Cliente cliente) {
        if (cliente.getId() == null) {
            cliente.atribuirId(UUID.randomUUID());
        }
        ClienteJpaEntity salvo = jpaRepository.save(ClientePersistenceMapper.toJpa(cliente));
        return ClientePersistenceMapper.toDomain(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findById(UUID id) {
        return jpaRepository.findById(id).map(ClientePersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findByCpf(String cpf) {
        return jpaRepository.findByCpf(cpf).map(ClientePersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findByTelefone(String telefone) {
        return jpaRepository.findByTelefoneDigits(telefone).map(ClientePersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> findByEmailIgnoreCase(String email) {
        return jpaRepository.findByEmailNormalizado(email).map(ClientePersistenceMapper::toDomain);
    }
}
