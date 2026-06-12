package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.exceptionhandler.ApiExceptionHandler.Problem;
import br.com.farmacia.application.financeiro.usecase.ConsultarContextoPdvUseCase; // H-11: controller agora depende da camada de aplicação, não do domínio
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller REST para contexto operacional de PDV (front-end).
 */
@RestController
@RequestMapping(path = "/api/v1/pdv", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "PDV", description = "Contexto operacional do ponto de venda")
@SecurityRequirement(name = "bearerAuth")
public class PdvController {

    private final ConsultarContextoPdvUseCase consultarContextoPdvUseCase; // H-11: repositórios de domínio removidos do controller

    @GetMapping("/contexto")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(
        summary = "Contexto do PDV",
        description = "Retorna o PDV operacional e se há caixa aberto — usado pelo front-end para montar vendas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contexto retornado"),
        @ApiResponse(responseCode = "404", description = "PDV não encontrado",
            content = @Content(schema = @Schema(implementation = Problem.class)))
    })
    public PdvContextoModel contexto(
            @RequestParam(defaultValue = "PDV-01") String numero) {

        ConsultarContextoPdvUseCase.Output out = consultarContextoPdvUseCase.executar(numero); // H-11: lógica delegada ao use case
        return new PdvContextoModel(out.pdvId(), out.numero(), out.descricao(), out.status(), out.caixaAberto());
    }

    public record PdvContextoModel(
        UUID pdvId,
        String numero,
        String descricao,
        String status,
        boolean caixaAberto
    ) {}
}
