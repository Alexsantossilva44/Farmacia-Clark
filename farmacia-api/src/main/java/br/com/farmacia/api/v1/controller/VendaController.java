package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.VendaAssembler;
import br.com.farmacia.api.v1.model.VendaModel;
import br.com.farmacia.api.v1.model.input.VendaInput;
import br.com.farmacia.api.exceptionhandler.ApiExceptionHandler.Problem;
import br.com.farmacia.application.venda.usecase.RealizarVendaUseCase;
import br.com.farmacia.application.venda.usecase.ConsultarVendaUseCase;
import br.com.farmacia.application.venda.usecase.CancelarVendaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller REST para operações de Venda.
 *
 * <p><b>Heurística AlgaWorks</b>:</p>
 * <ul>
 *   <li>Controller delega 100% para o Use Case via Assembler —
 *       sem {@code if}, sem cálculo, sem acesso a repositório.</li>
 *   <li>O retorno do Use Case ({@code Output record}) é convertido
 *       para o Model pela assembler antes de sair.</li>
 *   <li>Filtros de data/status na listagem usam parâmetros opcionais
 *       com {@code @RequestParam(required = false)}.</li>
 * </ul>
 */
@RestController
@RequestMapping(path = "/api/v1/vendas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Vendas", description = "Operações de PDV, realização de vendas e consulta de histórico")
@SecurityRequirement(name = "bearerAuth")
public class VendaController {

    private final RealizarVendaUseCase  realizarVendaUseCase;
    private final ConsultarVendaUseCase consultarVendaUseCase;
    private final CancelarVendaUseCase  cancelarVendaUseCase;
    private final VendaAssembler        vendaAssembler;

    // ─── POST /vendas — Realizar venda ────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(
        summary = "Realizar venda",
        description = """
            Processa uma venda completa no PDV.
            
            **Regras aplicadas automaticamente:**
            - Seleção de lote por FEFO (First Expired, First Out)
            - Validação de PMC (Preço Máximo ao Consumidor)
            - Verificação de receita para medicamentos controlados
            - CPF obrigatório para controlados e antimicrobianos
            - Registro SNGPC assíncrono para medicamentos controlados
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Venda realizada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Receita obrigatória ausente ou CPF não informado",
            content = @Content(schema = @Schema(implementation = Problem.class))),
        @ApiResponse(responseCode = "409", description = "Caixa fechado ou estoque insuficiente",
            content = @Content(schema = @Schema(implementation = Problem.class))),
        @ApiResponse(responseCode = "422", description = "Preço acima do PMC ou pagamento insuficiente",
            content = @Content(schema = @Schema(implementation = Problem.class)))
    })
    public VendaRealizadaResponse realizar(@RequestBody @Valid VendaInput vendaInput) {
        var useCaseInput = vendaAssembler.toUseCaseInput(vendaInput);
        var output = realizarVendaUseCase.executar(useCaseInput);

        return new VendaRealizadaResponse(
            output.vendaId(),
            output.numeroCupom(),
            output.total(),
            output.avisos()
        );
    }

    // ─── GET /vendas/{id} — Buscar venda ──────────────────────────────────

    @GetMapping("/{vendaId}")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Buscar venda por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venda encontrada"),
        @ApiResponse(responseCode = "404", description = "Venda não encontrada",
            content = @Content(schema = @Schema(implementation = Problem.class)))
    })
    public VendaModel buscar(
            @Parameter(description = "ID da venda") @PathVariable UUID vendaId) {

        return vendaAssembler.toModel(consultarVendaUseCase.buscarOuFalhar(vendaId));
    }

    // ─── GET /vendas — Listar com filtros ────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(
        summary = "Listar vendas com filtros",
        description = "Retorna vendas paginadas. Filtros disponíveis: data, cliente, PDV e status."
    )
    public Page<VendaModel> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,

            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) UUID pdvId,
            @RequestParam(required = false) String status,

            @PageableDefault(size = 20, sort = "dataHora",
                direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {

        return consultarVendaUseCase
            .listarComFiltro(dataInicio, dataFim, clienteId, pdvId, status, pageable)
            .map(vendaAssembler::toModel);
    }

    // ─── DELETE /vendas/{id} — Cancelar venda ────────────────────────────

    @DeleteMapping("/{vendaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(
        summary = "Cancelar venda",
        description = "Cancela uma venda e estorna o estoque dos lotes envolvidos. " +
                      "Apenas vendas do dia corrente podem ser canceladas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Venda cancelada e estoque estornado"),
        @ApiResponse(responseCode = "409", description = "Venda não pode ser cancelada (data ou status)")
    })
    public void cancelar(
            @PathVariable UUID vendaId,
            @RequestParam @jakarta.validation.constraints.NotBlank String motivo) { // M-02: motivo obrigatório — cancelamento sem justificativa não deve ser aceito

        cancelarVendaUseCase.executar(vendaId, motivo);
    }

    // ─── Response record (padrão AlgaWorks: response específico para ação)

    /**
     * Resposta da ação de realizar venda.
     *
     * <p>AlgaWorks: para ações (POST que disparam fluxos complexos),
     * o response pode diferir do Model completo — retorna só
     * o que o frontend precisa imediatamente após a ação.</p>
     */
    public record VendaRealizadaResponse(
        UUID vendaId,
        String numeroCupom,
        java.math.BigDecimal total,
        java.util.List<String> avisos
    ) {}
}
