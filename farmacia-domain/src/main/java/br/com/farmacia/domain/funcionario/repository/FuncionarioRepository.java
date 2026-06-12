package br.com.farmacia.domain.funcionario.repository;

import br.com.farmacia.domain.funcionario.entity.Funcionario;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para funcionários.
 *
 * @author Alex Silva e Claude
 */
public interface FuncionarioRepository {
    Funcionario save(Funcionario funcionario);
    Optional<Funcionario> findById(UUID id);
    Optional<Funcionario> findByEmail(String email);
}
