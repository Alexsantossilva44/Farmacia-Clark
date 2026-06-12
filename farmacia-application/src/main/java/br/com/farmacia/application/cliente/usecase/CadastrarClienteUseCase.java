package br.com.farmacia.application.cliente.usecase;

import br.com.farmacia.application.cliente.ClienteContatoService;
import br.com.farmacia.domain.cliente.ClienteValidacao;
import br.com.farmacia.domain.cliente.exception.CpfClienteDuplicadoException;
import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.cliente.repository.ClienteRepository;
import br.com.farmacia.domain.cliente.valueobject.EnderecoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cadastra cliente da farmácia.
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CadastrarClienteUseCase {

    private final ClienteRepository clienteRepository;
    private final ClienteContatoService clienteContatoService;

    @Transactional
    public Cliente executar(Input input) {
        ClienteValidacao.validarNome(input.nome());
        ClienteValidacao.validarCpf(input.cpf());
        ClienteValidacao.validarDataNascimentoObrigatoria(input.dataNascimento());
        ClienteValidacao.validarSexo(input.sexo());
        ClienteValidacao.validarTelefoneObrigatorio(input.telefone());
        ClienteValidacao.validarEmailObrigatorio(input.email());
        ClienteValidacao.validarEnderecoObrigatorio(input.endereco());

        String cpfLimpo = input.cpf().replaceAll("\\D", "");
        if (clienteRepository.findByCpf(cpfLimpo).isPresent()) {
            throw new CpfClienteDuplicadoException();
        }

        clienteContatoService.validarTelefoneUnico(input.telefone(), null);
        clienteContatoService.validarEmailUnico(input.email(), null);

        String telefoneNormalizado = normalizarTelefoneOuNulo(input.telefone());
        String emailNormalizado = normalizarEmailOuNulo(input.email());

        Cliente cliente = Cliente.builder()
            .nome(ClienteValidacao.normalizarNome(input.nome()))
            .cpf(cpfLimpo)
            .dataNascimento(input.dataNascimento())
            .sexo(input.sexo())
            .telefone(telefoneNormalizado)
            .email(emailNormalizado)
            .endereco(input.endereco())
            .alergias(input.alergias())
            .observacoes(input.observacoes())
            .dataCadastro(LocalDateTime.now())
            .ativo(true)
            .build();

        Cliente salvo = clienteRepository.save(cliente);
        log.info("Cliente cadastrado: {} ({})", salvo.getNome(), salvo.getCpf());
        return salvo;
    }

    private static String normalizarTelefoneOuNulo(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }
        return ClienteValidacao.normalizarTelefone(telefone);
    }

    private static String normalizarEmailOuNulo(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return ClienteValidacao.normalizarEmail(email);
    }

    public record Input(
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String sexo,
        String telefone,
        String email,
        EnderecoVO endereco,
        String alergias,
        String observacoes
    ) {}
}
