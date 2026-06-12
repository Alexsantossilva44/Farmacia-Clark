package br.com.farmacia.qa.unit;

import br.com.farmacia.domain.cliente.ClienteValidacao;
import br.com.farmacia.domain.cliente.exception.ClienteDadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ClienteValidacao")
class ClienteValidacaoTest {

    @Test
    @DisplayName("rejeita nome com mais de 100 caracteres")
    void rejeita_nome_muito_longo() {
        // Garante regra V6: clientes.nome e ClienteValidacao.NOME_MAX_LENGTH = 100.
        String nomeLongo = "Maria " + "Silva".repeat(20);
        assertThatThrownBy(() -> ClienteValidacao.validarNome(nomeLongo))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("100 caracteres");
    }

    @Test
    @DisplayName("rejeita nome com caracteres especiais")
    void rejeita_nome_com_caracteres_especiais() {
        assertThatThrownBy(() -> ClienteValidacao.validarNome("Jorge Mace*+do"))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("letras");
    }

    @Test
    @DisplayName("rejeita nome com múltiplos hífens consecutivos")
    void rejeita_nome_com_hifen() {
        // D-07: "---" consecutivos não são parte válida de nome — regex rejeita
        assertThatThrownBy(() -> ClienteValidacao.validarNome("Jorge ---Macedo"))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("letras");
    }

    @Test
    @DisplayName("aceita nome com hífen simples entre partes do nome")
    void aceita_nome_com_hifen() {
        // D-07: hífen simples é válido em nomes compostos brasileiros
        assertThatCode(() -> ClienteValidacao.validarNome("Maria da Silva-Santos"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("aceita nome com acento e preposição")
    void aceita_nome_valido() {
        assertThatCode(() -> ClienteValidacao.validarNome("Maria da Silva"))
            .doesNotThrowAnyException();
        assertThatCode(() -> ClienteValidacao.validarNome("José Antônio"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejeita CPF com dígitos verificadores inválidos")
    void rejeita_cpf_invalido() {
        assertThatThrownBy(() -> ClienteValidacao.validarCpf("12345678901"))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("CPF inválido");
    }

    @Test
    @DisplayName("rejeita data futura")
    void rejeita_data_futura() {
        assertThatThrownBy(() -> ClienteValidacao.validarDataNascimento(LocalDate.now().plusDays(1)))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("futura");
    }

    @Test
    @DisplayName("rejeita cliente menor de 18 anos")
    void rejeita_menor_de_idade() {
        LocalDate nascimento = LocalDate.now().minusYears(17);
        assertThatThrownBy(() -> ClienteValidacao.validarDataNascimento(nascimento))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("18 anos");
    }

    @Test
    @DisplayName("aceita cliente que completou 18 anos")
    void aceita_maior_de_idade() {
        LocalDate nascimento = LocalDate.now().minusYears(18);
        assertThatCode(() -> ClienteValidacao.validarDataNascimento(nascimento))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cadastro exige data de nascimento")
    void cadastro_exige_data_nascimento() {
        assertThatThrownBy(() -> ClienteValidacao.validarDataNascimentoObrigatoria(null))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("obrigatória");
    }

    @Test
    @DisplayName("rejeita endereço sem logradouro")
    void rejeita_endereco_sem_logradouro() {
        assertThatThrownBy(() -> ClienteValidacao.validarEnderecoObrigatorio(
            br.com.farmacia.domain.cliente.valueobject.EnderecoVO.builder()
                .bairro("Centro")
                .cep("01310100")
                .cidade("Sao Paulo")
                .uf("SP")
                .build()))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("Logradouro");
    }

    @Test
    @DisplayName("rejeita endereço sem UF")
    void rejeita_endereco_sem_uf() {
        assertThatThrownBy(() -> ClienteValidacao.validarEnderecoObrigatorio(
            br.com.farmacia.domain.cliente.valueobject.EnderecoVO.builder()
                .logradouro("Av Paulista")
                .bairro("Centro")
                .cep("01310100")
                .cidade("Sao Paulo")
                .build()))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("UF");
    }

    @Test
    @DisplayName("rejeita endereço sem cidade")
    void rejeita_endereco_sem_cidade() {
        assertThatThrownBy(() -> ClienteValidacao.validarEnderecoObrigatorio(
            br.com.farmacia.domain.cliente.valueobject.EnderecoVO.builder()
                .logradouro("Av Paulista")
                .bairro("Centro")
                .cep("01310100")
                .uf("SP")
                .build()))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("Cidade");
    }

    @Test
    @DisplayName("rejeita telefone com poucos dígitos")
    void rejeita_telefone_invalido() {
        assertThatThrownBy(() -> ClienteValidacao.validarTelefone("12345"))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("Telefone");
    }

    @Test
    @DisplayName("rejeita e-mail com espaços")
    void rejeita_email_com_espacos() {
        assertThatThrownBy(() -> ClienteValidacao.validarEmail("jorge@gmail.com "))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("espaços");
    }

    @Test
    @DisplayName("rejeita e-mail inválido")
    void rejeita_email_invalido() {
        assertThatThrownBy(() -> ClienteValidacao.validarEmail("email-invalido"))
            .isInstanceOf(ClienteDadosInvalidosException.class)
            .hasMessageContaining("E-mail");
    }
}
