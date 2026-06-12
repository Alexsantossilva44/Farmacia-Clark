package br.com.farmacia.api.v1.model.input;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO de entrada para autenticação de funcionário.
 *
 * <p><b>Heurística AlgaWorks</b>: DTOs de entrada usam o sufixo {@code Input}
 * e expõem apenas o necessário — credenciais para troca por um token JWT.</p>
 */
@Getter
@Setter
@Schema(description = "Credenciais para autenticação")
public class LoginInput {

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Schema(description = "E-mail corporativo do funcionário", example = "admin@farmacia.com")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha de acesso", example = "admin123")
    private String senha;
}
