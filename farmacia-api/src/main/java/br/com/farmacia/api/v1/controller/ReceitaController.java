package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.ReceitaAssembler;
import br.com.farmacia.api.v1.model.ReceitaModel;
import br.com.farmacia.api.v1.model.input.ReceitaInput;
import br.com.farmacia.api.v1.model.input.ValidarReceitaInput;
import br.com.farmacia.application.receituario.usecase.CadastrarReceitaUseCase;
import br.com.farmacia.application.receituario.usecase.ConsultarReceitaUseCase;
import br.com.farmacia.application.receituario.usecase.ValidarReceitaUseCase;
import br.com.farmacia.domain.funcionario.repository.FarmaceuticoRepository;
import br.com.farmacia.domain.receituario.exception.FarmaceuticoNaoVinculadoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/receitas", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Receituário", description = "Cadastro e validação de receitas")
@SecurityRequirement(name = "bearerAuth")
public class ReceitaController {

    private final CadastrarReceitaUseCase cadastrarReceitaUseCase;
    private final ConsultarReceitaUseCase consultarReceitaUseCase;
    private final ValidarReceitaUseCase validarReceitaUseCase;
    private final FarmaceuticoRepository farmaceuticoRepository;
    private final ReceitaAssembler receitaAssembler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Cadastrar receita (status PENDENTE)")
    public ReceitaModel cadastrar(@RequestBody @Valid ReceitaInput input) {
        var receita = cadastrarReceitaUseCase.executar(new CadastrarReceitaUseCase.Input(
            input.getNumeroReceita(),
            input.getTipo(),
            input.getDataEmissao(),
            input.getPrescritorId(),
            input.getClienteId(),
            input.getCid()
        ));
        return receitaAssembler.toModel(receita);
    }

    @GetMapping("/{receitaId}")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Buscar receita por ID")
    public ReceitaModel buscar(@PathVariable UUID receitaId) {
        return receitaAssembler.toModel(consultarReceitaUseCase.buscarOuFalhar(receitaId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Buscar receita por número")
    public ReceitaModel buscarPorNumero(@RequestParam String numero) {
        return receitaAssembler.toModel(consultarReceitaUseCase.buscarPorNumeroOuFalhar(numero));
    }

    @PutMapping("/{receitaId}/validar")
    @PreAuthorize("hasAnyRole('FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Validar receita (aprovar ou rejeitar)")
    public ValidarReceitaUseCase.Output validar(
            @PathVariable UUID receitaId,
            @RequestBody @Valid ValidarReceitaInput input,
            @AuthenticationPrincipal Jwt jwt) {

        UUID funcionarioId = UUID.fromString(jwt.getClaimAsString("funcionarioId"));
        UUID farmaceuticoId = farmaceuticoRepository.findByFuncionarioId(funcionarioId)
            .orElseThrow(() -> new FarmaceuticoNaoVinculadoException(funcionarioId))
            .getId();

        var itens = input.getItens().stream()
            .map(i -> new ValidarReceitaUseCase.Input.ItemValidacao(i.getMedicamentoId(), i.getQuantidade()))
            .toList();

        return validarReceitaUseCase.executar(new ValidarReceitaUseCase.Input(receitaId, farmaceuticoId, itens));
    }
}
