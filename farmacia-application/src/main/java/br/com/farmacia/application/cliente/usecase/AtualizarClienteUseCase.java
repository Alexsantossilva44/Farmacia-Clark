package br.com.farmacia.application.cliente.usecase;

import br.com.farmacia.application.cliente.ClienteContatoService;
import br.com.farmacia.domain.cliente.ClienteValidacao;
import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.cliente.exception.ClienteNaoEncontradoException;
import br.com.farmacia.domain.cliente.repository.ClienteRepository;
import br.com.farmacia.domain.cliente.valueobject.EnderecoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Atualiza dados cadastrais de cliente.
 *
 * @author Alex Silva e Claude
 */
@Service
@RequiredArgsConstructor
public class AtualizarClienteUseCase {

    private final ClienteRepository clienteRepository;
    private final ClienteContatoService clienteContatoService;

    @Transactional
    public Cliente executar(UUID id, Input input) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new ClienteNaoEncontradoException(id));

        String nome = null;
        if (input.nome() != null) {
            ClienteValidacao.validarNome(input.nome());
            nome = ClienteValidacao.normalizarNome(input.nome());
        }

        ClienteValidacao.validarDataNascimentoObrigatoria(input.dataNascimento());
        ClienteValidacao.validarSexo(input.sexo());
        ClienteValidacao.validarTelefoneObrigatorio(input.telefone());
        ClienteValidacao.validarEmailObrigatorio(input.email());
        ClienteValidacao.validarEnderecoObrigatorio(input.endereco());

        clienteContatoService.validarTelefoneUnico(input.telefone(), id);
        clienteContatoService.validarEmailUnico(input.email(), id);

        String telefone = ClienteValidacao.normalizarTelefone(input.telefone());
        String email = ClienteValidacao.normalizarEmail(input.email());

        cliente.atualizar(
            nome,
            input.dataNascimento(),
            input.sexo(),
            telefone,
            email,
            input.endereco(),
            input.alergias(),
            input.observacoes(),
            input.ativo()
        );

        return clienteRepository.save(cliente);
    }

    public record Input(
        String nome,
        LocalDate dataNascimento,
        String sexo,
        String telefone,
        String email,
        EnderecoVO endereco,
        String alergias,
        String observacoes,
        Boolean ativo
    ) {}
}
