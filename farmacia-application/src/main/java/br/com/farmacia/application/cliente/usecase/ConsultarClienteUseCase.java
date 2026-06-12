package br.com.farmacia.application.cliente.usecase;

import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.cliente.exception.ClienteNaoEncontradoException;
import br.com.farmacia.domain.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consulta clientes por id ou CPF.
 *
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class ConsultarClienteUseCase {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public Cliente buscarOuFalhar(UUID id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new ClienteNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public Cliente buscarPorCpfOuFalhar(String cpf) {
        String cpfLimpo = cpf.replaceAll("\\D", "");
        return clienteRepository.findByCpf(cpfLimpo)
            .orElseThrow(() -> new ClienteNaoEncontradoException(cpfLimpo));
    }
}
