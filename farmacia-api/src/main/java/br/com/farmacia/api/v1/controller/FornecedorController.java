package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.CompraAssembler;
import br.com.farmacia.api.v1.model.input.FornecedorInput;
import br.com.farmacia.api.v1.model.output.FornecedorModel;
import br.com.farmacia.application.compra.usecase.CadastrarFornecedorUseCase;
import br.com.farmacia.application.compra.usecase.ListarFornecedoresUseCase;
import br.com.farmacia.domain.compra.exception.CnpjDuplicadoException;
import br.com.farmacia.domain.compra.exception.CnpjInvalidoException;
import br.com.farmacia.infrastructure.persistence.compra.FornecedorJpaEntity;
import br.com.farmacia.infrastructure.persistence.compra.FornecedorJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/fornecedores", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Fornecedores", description = "Cadastro de fornecedores")
@SecurityRequirement(name = "bearerAuth")
public class FornecedorController {

    private final ListarFornecedoresUseCase listarFornecedoresUseCase;
    private final CadastrarFornecedorUseCase cadastrarFornecedorUseCase;
    private final CompraAssembler compraAssembler;
    private final FornecedorJpaRepository fornecedorJpaRepository;

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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Atualizar fornecedor")
    public FornecedorModel atualizar(@PathVariable UUID id, @RequestBody FornecedorInput input) {
        FornecedorJpaEntity entity = fornecedorJpaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fornecedor não encontrado"));

        if (input.getRazaoSocial() == null || input.getRazaoSocial().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Lembre-se: Campo obrigatório.");
        }

        String cnpj = input.getCnpj() != null ? input.getCnpj().replaceAll("\\D", "") : "";
        if (cnpj.length() != 14) {
            throw new CnpjInvalidoException();
        }
        if (!cnpj.equals(entity.getCnpj())) {
            fornecedorJpaRepository.findByCnpj(cnpj)
                .ifPresent(e -> { throw new CnpjDuplicadoException(cnpj); });
        }

        entity.setRazaoSocial(input.getRazaoSocial().trim());
        entity.setNomeFantasia(
            input.getNomeFantasia() != null && !input.getNomeFantasia().isBlank()
                ? input.getNomeFantasia().trim() : null);
        entity.setCnpj(cnpj);

        FornecedorJpaEntity saved = fornecedorJpaRepository.save(entity);
        FornecedorModel model = new FornecedorModel();
        model.setId(saved.getId());
        model.setRazaoSocial(saved.getRazaoSocial());
        model.setNomeFantasia(saved.getNomeFantasia());
        model.setCnpj(saved.getCnpj());
        model.setAtivo(saved.getAtivo());
        return model;
    }
}
