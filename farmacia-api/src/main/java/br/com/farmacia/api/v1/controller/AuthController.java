package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.exceptionhandler.ApiExceptionHandler.Problem;
import br.com.farmacia.api.v1.model.TokenModel;
import br.com.farmacia.api.v1.model.input.LoginInput;
import br.com.farmacia.infrastructure.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.farmacia.domain.funcionario.repository.FarmaceuticoRepository;

import java.util.UUID;

/**
 * Controller REST de autenticação.
 *
 * <p><b>Heurística AlgaWorks</b>: o controller é fino — recebe as credenciais,
 * delega a autenticação/emissão do token ao {@link TokenService} e devolve o
 * {@link TokenModel}. Este endpoint é público (ver {@code SecurityConfig}).</p>
 *
 * @author Alex Silva e Claude
 */
@RestController
@RequestMapping(path = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Emissão de tokens JWT para acesso à API")
public class AuthController {

    private final TokenService tokenService;
    private final FarmaceuticoRepository farmaceuticoRepository;

    @GetMapping("/contexto")
    @Operation(
        summary = "Contexto do usuário autenticado",
        description = "Retorna perfil e se o funcionário possui registro de farmacêutico (CRF) "
                    + "necessário para validar receitas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contexto retornado"),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = Problem.class)))
    })
    
    public AuthContextoModel contexto(@AuthenticationPrincipal Jwt jwt) {
        UUID funcionarioId = UUID.fromString(jwt.getClaimAsString("funcionarioId"));
        var farmaceutico = farmaceuticoRepository.findByFuncionarioId(funcionarioId);

        String role = jwt.getClaimAsStringList("roles") != null && !jwt.getClaimAsStringList("roles").isEmpty()
            ? jwt.getClaimAsStringList("roles").getFirst()
            : null;

        return new AuthContextoModel(
            jwt.getSubject(),
            jwt.getClaimAsString("nome"),
            role,
            farmaceutico.isPresent(),
            farmaceutico.map(f -> f.getCrf()).orElse(null),
            farmaceutico.map(f -> f.getUfCrf()).orElse(null)
        );
    }

    @PostMapping("/token")
    @Operation(
        summary = "Autenticar e obter token",
        description = "Valida e-mail e senha do funcionário e retorna um token JWT (RS256) "
                    + "a ser enviado no header Authorization: Bearer <token>."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
            content = @Content(schema = @Schema(implementation = Problem.class)))
    })
    public TokenModel token(@RequestBody @Valid LoginInput loginInput) {
        var resultado = tokenService.autenticar(loginInput.getEmail(), loginInput.getSenha());
        return new TokenModel(resultado.token(), resultado.tipo(), resultado.expiraEmSegundos());
    }

    public record AuthContextoModel(
        String email,
        String nome,
        String role,
        boolean possuiRegistroFarmaceutico,
        String crf,
        String ufCrf
    ) {}
}
