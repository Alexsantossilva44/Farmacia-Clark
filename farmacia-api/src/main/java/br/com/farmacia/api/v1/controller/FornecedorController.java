package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.CompraAssembler;
import br.com.farmacia.api.v1.model.input.FornecedorInput;
import br.com.farmacia.api.v1.model.input.NotaFiscalEntradaInput;
import br.com.farmacia.api.v1.model.output.FornecedorModel;
import br.com.farmacia.api.v1.model.output.NotaFiscalEntradaModel;
import br.com.farmacia.application.compra.usecase.CadastrarFornecedorUseCase;
import br.com.farmacia.application.compra.usecase.ListarFornecedoresUseCase;
import br.com.farmacia.application.compra.usecase.ListarNotasFiscaisEntradaUseCase;
import br.com.farmacia.application.compra.usecase.RegistrarNotaFiscalEntradaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/fornecedores", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Cadastro de fornecedores")
@SecurityRequirement(name = "bearerAuth")
public class FornecedorController {

    private final ListarFornecedoresUseCase listarFornecedoresUseCase;
    private final CadastrarFornecedorUseCase cadastrarFornecedorUseCase;
    private final CompraAssembler compraAssembler;

    @GetMapping
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Listar fornecedores ativos")
    public List<FornecedorModel> listar() {
        return compraAssembler.toFornecedorCollection(listarFornecedoresUseCase.executar());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Cadastrar fornecedor")
    public FornecedorModel cadastrar(@RequestBody @Valid FornecedorInput input) {
        return compraAssembler.toModel(cadastrarFornecedorUseCase.executar(
            new CadastrarFornecedorUseCase.Input(
                input.getRazaoSocial(),
                input.getNomeFantasia(),
                input.getCnpj()
            )));
    }
}
