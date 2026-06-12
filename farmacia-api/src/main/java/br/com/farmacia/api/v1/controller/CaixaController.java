package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.CaixaAssembler;
import br.com.farmacia.api.v1.model.CaixaModel;
import br.com.farmacia.api.v1.model.input.AbrirCaixaInput;
import br.com.farmacia.api.v1.model.input.FecharCaixaInput;
import br.com.farmacia.application.financeiro.usecase.AbrirCaixaUseCase;
import br.com.farmacia.application.financeiro.usecase.ConsultarCaixaUseCase;
import br.com.farmacia.application.financeiro.usecase.FecharCaixaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/caixa", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Caixa", description = "Abertura, fechamento e consulta de caixa por PDV")
@SecurityRequirement(name = "bearerAuth")
public class CaixaController {

    private final AbrirCaixaUseCase abrirCaixaUseCase;
    private final FecharCaixaUseCase fecharCaixaUseCase;
    private final ConsultarCaixaUseCase consultarCaixaUseCase;
    private final CaixaAssembler caixaAssembler;

    @GetMapping("/aberto")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Consultar caixa aberto do PDV")
    public ResponseEntity<CaixaModel> consultarAberto(@RequestParam UUID pdvId) {
        // M-03: orElse(null) retornava 200 com body null; agora retorna 404 quando não há caixa aberto
        return consultarCaixaUseCase.buscarCaixaAbertoPorPdv(pdvId)
            .map(caixaAssembler::toModel)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BALCONISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Abrir caixa no PDV")
    public CaixaAberturaResponse abrir(@RequestBody @Valid AbrirCaixaInput input) {
        var output = abrirCaixaUseCase.executar(new AbrirCaixaUseCase.Input(
            input.getPdvId(),
            input.getFuncionarioId(),
            input.getSaldoAbertura()
        ));
        return new CaixaAberturaResponse(output.caixaId(), output.pdvId(), output.pdvNumero());
    }

    @PostMapping("/fechar")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Fechar caixa aberto do PDV")
    public CaixaFechamentoResponse fechar(@RequestBody @Valid FecharCaixaInput input) {
        var output = fecharCaixaUseCase.executar(new FecharCaixaUseCase.Input(
            input.getPdvId(),
            input.getObservacao()
        ));
        return new CaixaFechamentoResponse(
            output.caixaId(), output.pdvId(), output.pdvNumero(), output.saldoFechamento());
    }

    public record CaixaAberturaResponse(UUID caixaId, UUID pdvId, String pdvNumero) {}

    public record CaixaFechamentoResponse(
        UUID caixaId, UUID pdvId, String pdvNumero, java.math.BigDecimal saldoFechamento) {}
}
