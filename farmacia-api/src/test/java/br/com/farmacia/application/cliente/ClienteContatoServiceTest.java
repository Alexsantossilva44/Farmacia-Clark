package br.com.farmacia.application.cliente;

// ClienteDadosInvalidosException removida: validação de formato não é mais responsabilidade
// deste serviço — o use case chamador já garante o formato antes de invocar validar*Unico.
import br.com.farmacia.domain.cliente.exception.EmailClienteDuplicadoException;
import br.com.farmacia.domain.cliente.exception.TelefoneClienteDuplicadoException;
import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.cliente.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteContatoService")
class ClienteContatoServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteContatoService clienteContatoService;

    private static final UUID CLIENTE_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CLIENTE_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    @DisplayName("ignora telefone em branco na validação de unicidade")
    void telefone_em_branco_nao_valida() {
        assertThatCode(() -> clienteContatoService.validarTelefoneUnico("  ", null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejeita telefone duplicado de outro cliente")
    void rejeita_telefone_de_outro_cliente() {
        when(clienteRepository.findByTelefone("11999998888"))
            .thenReturn(Optional.of(Cliente.builder().id(CLIENTE_B).build()));

        assertThatThrownBy(() -> clienteContatoService.validarTelefoneUnico("(11) 99999-8888", CLIENTE_A))
            .isInstanceOf(TelefoneClienteDuplicadoException.class)
            .hasMessage("Telefone já está em uso.");
    }

    @Test
    @DisplayName("permite telefone do próprio cliente na edição")
    void permite_telefone_do_proprio_cliente() {
        when(clienteRepository.findByTelefone("11999998888"))
            .thenReturn(Optional.of(Cliente.builder().id(CLIENTE_A).build()));

        assertThatCode(() -> clienteContatoService.validarTelefoneUnico("11999998888", CLIENTE_A))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejeita e-mail duplicado de outro cliente")
    void rejeita_email_de_outro_cliente() {
        when(clienteRepository.findByEmailIgnoreCase("maria@email.com"))
            .thenReturn(Optional.of(Cliente.builder().id(CLIENTE_B).build()));

        assertThatThrownBy(() -> clienteContatoService.validarEmailUnico("Maria@Email.com", CLIENTE_A))
            .isInstanceOf(EmailClienteDuplicadoException.class)
            .hasMessage("E-mail já está em uso.");
    }

    @Test
    @DisplayName("verificarDisponibilidade retorna false quando telefone já existe")
    void disponibilidade_telefone_ocupado() {
        when(clienteRepository.findByTelefone("11988887777"))
            .thenReturn(Optional.of(Cliente.builder().id(CLIENTE_B).build()));

        var result = clienteContatoService.verificarDisponibilidade("11988887777", null, null);

        assertThat(result.telefoneDisponivel()).isFalse();
        assertThat(result.emailDisponivel()).isTrue();
    }

    // Teste de formato inválido removido: ClienteContatoService não valida formato —
    // essa responsabilidade ficou nos use cases (CadastrarClienteUseCase / AtualizarClienteUseCase).

    @Test
    @DisplayName("ignora e-mail em branco na validação de unicidade")
    void email_em_branco_nao_valida() {
        // e-mail é opcional: valor em branco não deve consultar o banco nem lançar exceção
        assertThatCode(() -> clienteContatoService.validarEmailUnico("  ", null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("permite e-mail do próprio cliente na edição")
    void permite_email_do_proprio_cliente() {
        // ao editar, o e-mail atual do cliente não deve ser considerado duplicata dele mesmo
        when(clienteRepository.findByEmailIgnoreCase("maria@email.com"))
            .thenReturn(Optional.of(Cliente.builder().id(CLIENTE_A).build()));

        assertThatCode(() -> clienteContatoService.validarEmailUnico("Maria@Email.com", CLIENTE_A))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejeita telefone duplicado no cadastro novo (excluirClienteId nulo)")
    void rejeita_telefone_duplicado_no_cadastro_novo() {
        // excluirClienteId = null → cadastro novo; qualquer telefone já existente é duplicata
        when(clienteRepository.findByTelefone("11999998888"))
            .thenReturn(Optional.of(Cliente.builder().id(CLIENTE_B).build()));

        assertThatThrownBy(() -> clienteContatoService.validarTelefoneUnico("(11) 99999-8888", null))
            .isInstanceOf(TelefoneClienteDuplicadoException.class)
            .hasMessage("Telefone já está em uso.");
    }
}
