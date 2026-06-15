package br.com.farmacia.domain.cliente.repository;

import br.com.farmacia.domain.cliente.entity.Cliente;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para clientes.
 *
 * @author Alex Silva e Claude
 */
public interface ClienteRepository {
    Cliente save(Cliente cliente);
    Optional<Cliente> findById(UUID id);
    Optional<Cliente> findByCpf(String cpf);
    Optional<Cliente> findByTelefone(String telefone);
    Optional<Cliente> findByEmailIgnoreCase(String email);
    List<Cliente> findByNome(String nome);
}
