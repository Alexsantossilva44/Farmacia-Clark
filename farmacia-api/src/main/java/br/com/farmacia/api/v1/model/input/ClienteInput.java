package br.com.farmacia.api.v1.model.input;

import br.com.farmacia.domain.cliente.valueobject.EnderecoVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ClienteInput {
    /**
     * Nome completo — máx. 100 caracteres (migration V6; antes 150).
     * Espelha {@link br.com.farmacia.domain.cliente.ClienteValidacao#NOME_MAX_LENGTH}.
     */
    @NotBlank(message = "Nome completo é obrigatório")
    @Size(max = 100, message = "Nome completo deve ter no máximo 100 caracteres")
    @Pattern(
        regexp = "^[\\p{L}]+(?: [\\p{L}]+)+$",
        message = "Nome deve conter apenas letras e um espaço entre nome e sobrenome")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos")
    private String cpf;

    @Past(message = "Data de nascimento deve ser anterior a hoje")
    private LocalDate dataNascimento;
    private String sexo;
    private String telefone;
    private String email;
    private EnderecoVO endereco;
    private String alergias;
    private String observacoes;
}
