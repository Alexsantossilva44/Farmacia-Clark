package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.MedicamentoAssembler;
import br.com.farmacia.api.v1.model.MedicamentoModel;
import br.com.farmacia.api.v1.model.input.MedicamentoInput;
import br.com.farmacia.api.exceptionhandler.ApiExceptionHandler;
import br.com.farmacia.application.medicamento.usecase.CadastrarMedicamentoUseCase;
import br.com.farmacia.application.medicamento.usecase.AtualizarMedicamentoUseCase;
import br.com.farmacia.application.medicamento.usecase.ConsultarMedicamentoUseCase;
import br.com.farmacia.application.medicamento.usecase.ExcluirMedicamentoUseCase;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para gerenciamento de medicamentos.
 *
 * <p><b>Heurística AlgaWorks</b>:</p>
 * <ul>
 *   <li>Controllers são finos: recebem a requisição, delegam ao use case, montam a resposta.</li>
 *   <li>Toda lógica de negócio fica no Use Case ou no Domain Service — nunca no controller.</li>
 *   <li>{@code @PreAuthorize} granular por método em vez de na configuração global.</li>
 *   <li>Paginação com {@code @PageableDefault} para valores sensatos sem parâmetros.</li>
 *   <li>Documentação OpenAPI inline via annotations (sem XML ou arquivos externos).</li>
 * </ul>
 */
@RestController
@RequestMapping(path = "/api/v1/medicamentos", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Medicamentos", description = "Gerenciamento do catálogo de medicamentos")
@SecurityRequirement(name = "bearerAuth")
public class MedicamentoController {

    private final CadastrarMedicamentoUseCase  cadastrarMedicamentoUseCase;
    private final AtualizarMedicamentoUseCase  atualizarMedicamentoUseCase;
    private final ConsultarMedicamentoUseCase  consultarMedicamentoUseCase;
    private final ExcluirMedicamentoUseCase    excluirMedicamentoUseCase;
    private final MedicamentoAssembler         medicamentoAssembler;

    // ─── GET /medicamentos ────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('BALCONISTA', 'ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(
        summary = "Listar medicamentos",
        description = "Retorna lista paginada de medicamentos. Filtro opcional por nome comercial, genérico ou EAN (`busca`)."
    )
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public Page<MedicamentoModel> listar(
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 20, sort = "nomeComercial", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return consultarMedicamentoUseCase.listarPaginado(pageable, busca)
            .map(medicamentoAssembler::toModel);
    }

    // ─── GET /medicamentos/{id} ───────────────────────────────────────────────

    @GetMapping("/{medicamentoId}")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Buscar medicamento por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Medicamento encontrado"),
        @ApiResponse(responseCode = "404", description = "Medicamento não encontrado",
            content = @Content(schema = @Schema(implementation = ApiExceptionHandler.Problem.class)))
    })
    public MedicamentoModel buscar(
            @Parameter(description = "ID do medicamento", required = true)
            @PathVariable UUID medicamentoId) {

        return medicamentoAssembler.toModel(
            consultarMedicamentoUseCase.buscarOuFalhar(medicamentoId)
        );
    }

    // ─── POST /medicamentos ───────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Cadastrar novo medicamento")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Medicamento cadastrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ApiExceptionHandler.Problem.class))),
        @ApiResponse(responseCode = "422", description = "Erro de validação de campos",
            content = @Content(schema = @Schema(implementation = ApiExceptionHandler.Problem.class)))
    })
    public MedicamentoModel adicionar(
            @RequestBody @Valid MedicamentoInput medicamentoInput) {

        var entity = medicamentoAssembler.toEntity(medicamentoInput);
        var salvo  = cadastrarMedicamentoUseCase.executar(entity);
        return medicamentoAssembler.toModel(salvo);
    }

    // ─── PUT /medicamentos/{id} ───────────────────────────────────────────────

    @PutMapping("/{medicamentoId}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Atualizar medicamento (substituição completa)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Medicamento atualizado"),
        @ApiResponse(responseCode = "404", description = "Medicamento não encontrado")
    })
    public MedicamentoModel atualizar(
            @PathVariable UUID medicamentoId,
            @RequestBody @Valid MedicamentoInput medicamentoInput) {

        var medicamentoAtual = consultarMedicamentoUseCase.buscarOuFalhar(medicamentoId);
        medicamentoAssembler.copyToEntity(medicamentoInput, medicamentoAtual);
        var atualizado = atualizarMedicamentoUseCase.executar(medicamentoAtual);
        return medicamentoAssembler.toModel(atualizado);
    }

    // ─── DELETE /medicamentos/{id} ────────────────────────────────────────────

    @DeleteMapping("/{medicamentoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Excluir medicamento (inativação lógica)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Medicamento inativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Medicamento não encontrado"),
        @ApiResponse(responseCode = "409", description = "Medicamento em uso, não pode ser excluído")
    })
    public void remover(@PathVariable UUID medicamentoId) {
        excluirMedicamentoUseCase.executar(medicamentoId);
    }
}
