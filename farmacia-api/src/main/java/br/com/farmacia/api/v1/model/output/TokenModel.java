package br.com.farmacia.api.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de saída da autenticação: o token JWT emitido e seus metadados.
 *
 * <p><b>Heurística AlgaWorks</b>: respostas de ação retornam apenas o que o
 * cliente precisa imediatamente — aqui, o token e como utilizá-lo.</p>
 */
@Schema(description = "Token de acesso emitido após autenticação")
public record TokenModel(

    @Schema(description = "Token JWT assinado (RS256)")
    String token,

    @Schema(description = "Tipo do token para o header Authorization", example = "Bearer")
    String tipo,

    @Schema(description = "Tempo de expiração do token, em segundos", example = "28800")
    long expiraEmSegundos
) {}
