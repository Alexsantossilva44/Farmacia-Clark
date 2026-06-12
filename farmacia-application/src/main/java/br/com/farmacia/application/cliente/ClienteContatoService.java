package br.com.farmacia.application.cliente;

import br.com.farmacia.domain.cliente.ClienteValidacao;
import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.cliente.exception.EmailClienteDuplicadoException;
import br.com.farmacia.domain.cliente.exception.TelefoneClienteDuplicadoException;
import br.com.farmacia.domain.cliente.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Valida unicidade de telefone e e-mail entre clientes.
 *
 * <p>Complementa {@code uk_clientes_telefone} / {@code uk_clientes_email} (V7):
 * a checagem em memória dá feedback rápido; o índice UNIQUE evita duplicata em concorrência.</p>
 */
@Service
@RequiredArgsConstructor
public class ClienteContatoService {

    private final ClienteRepository clienteRepository;

    // DisponibilidadeContato foi extraído para DisponibilidadeContato.java (mesmo pacote)
    // para que outras classes importem apenas o value object sem depender deste @Service.

    /** Usado pelo endpoint {@code GET /clientes/contato/disponivel} e pelo front no blur/submit. */
    @Transactional(readOnly = true)
    public DisponibilidadeContato verificarDisponibilidade(String telefone, String email, UUID excluirClienteId) {
        return new DisponibilidadeContato(
            telefoneDisponivel(telefone, excluirClienteId),
            emailDisponivel(email, excluirClienteId)
        );
    }

    /**
     * Valida que {@code telefone} não pertence a outro cliente.
     *
     * <p><b>Pré-condição:</b> formato já validado pelo chamador
     * (ex.: {@code ClienteValidacao.validarTelefoneObrigatorio});
     * este método verifica <em>apenas unicidade</em> — não repete a checagem de formato.</p>
     *
     * @param excluirClienteId ID do cliente em edição; {@code null} no cadastro novo.
     */
    @Transactional(readOnly = true)
    public void validarTelefoneUnico(String telefone, UUID excluirClienteId) {
        if (telefone == null || telefone.isBlank()) {
            return; // telefone opcional: sem valor não há duplicata para checar
        }
        // validação de formato removida daqui — é responsabilidade do use case chamador
        if (!telefoneDisponivel(telefone, excluirClienteId)) {
            throw new TelefoneClienteDuplicadoException();
        }
    }

    /**
     * Valida que {@code email} não pertence a outro cliente.
     *
     * <p><b>Pré-condição:</b> formato já validado pelo chamador
     * (ex.: {@code ClienteValidacao.validarEmailObrigatorio});
     * este método verifica <em>apenas unicidade</em> — não repete a checagem de formato.</p>
     *
     * @param excluirClienteId ID do cliente em edição; {@code null} no cadastro novo.
     */
    @Transactional(readOnly = true)
    public void validarEmailUnico(String email, UUID excluirClienteId) {
        if (email == null || email.isBlank()) {
            return; // e-mail opcional: sem valor não há duplicata para checar
        }
        // validação de formato removida daqui — é responsabilidade do use case chamador
        if (!emailDisponivel(email, excluirClienteId)) {
            throw new EmailClienteDuplicadoException();
        }
    }

    private boolean telefoneDisponivel(String telefone, UUID excluirClienteId) {
        String normalizado = ClienteValidacao.normalizarTelefone(telefone);
        if (normalizado.isBlank()) {
            return true;
        }
        return clienteRepository.findByTelefone(normalizado)
            .map(cliente -> pertenceAoCliente(cliente, excluirClienteId))
            .orElse(true);
    }

    private boolean emailDisponivel(String email, UUID excluirClienteId) {
        String normalizado = ClienteValidacao.normalizarEmail(email);
        if (normalizado.isBlank()) {
            return true;
        }
        return clienteRepository.findByEmailIgnoreCase(normalizado)
            .map(cliente -> pertenceAoCliente(cliente, excluirClienteId))
            .orElse(true);
    }

    /** Na edição, telefone/e-mail do próprio cliente não conta como duplicata. */
    private boolean pertenceAoCliente(Cliente cliente, UUID excluirClienteId) {
        return excluirClienteId != null && excluirClienteId.equals(cliente.getId());
    }
}
