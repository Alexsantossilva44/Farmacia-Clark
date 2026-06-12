package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.ClienteAssembler;
import br.com.farmacia.api.v1.model.output.DisponibilidadeContatoModel;
import br.com.farmacia.api.v1.model.ClienteModel;
import br.com.farmacia.api.v1.model.input.ClienteAtualizacaoInput;
import br.com.farmacia.api.v1.model.input.ClienteInput;
import br.com.farmacia.application.cliente.ClienteContatoService;
import br.com.farmacia.application.cliente.usecase.AtualizarClienteUseCase;
import br.com.farmacia.application.cliente.usecase.CadastrarClienteUseCase;
import br.com.farmacia.application.cliente.usecase.ConsultarClienteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro e consulta de clientes")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final CadastrarClienteUseCase cadastrarClienteUseCase;
    private final ConsultarClienteUseCase consultarClienteUseCase;
    private final AtualizarClienteUseCase atualizarClienteUseCase;
    private final ClienteContatoService clienteContatoService;
    private final ClienteAssembler clienteAssembler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Cadastrar cliente")
    public ClienteModel cadastrar(@RequestBody @Valid ClienteInput input) {
        var cliente = cadastrarClienteUseCase.executar(new CadastrarClienteUseCase.Input(
            input.getNome(),
            input.getCpf(),
            input.getDataNascimento(),
            input.getSexo(),
            input.getTelefone(),
            input.getEmail(),
            input.getEndereco(),
            input.getAlergias(),
            input.getObservacoes()
        ));
        return clienteAssembler.toModel(cliente);
    }

    @GetMapping("/cpf/{cpf}")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Buscar cliente por CPF")
    public ClienteModel buscarPorCpf(@PathVariable String cpf) {
        return clienteAssembler.toModel(consultarClienteUseCase.buscarPorCpfOuFalhar(cpf));
    }

    @GetMapping("/contato/disponivel")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Verificar disponibilidade de telefone e e-mail")
    public DisponibilidadeContatoModel verificarContatoDisponivel(
            @RequestParam(required = false) String telefone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) UUID excluirClienteId) {
        var disponibilidade = clienteContatoService.verificarDisponibilidade(
            telefone, email, excluirClienteId);
        DisponibilidadeContatoModel model = new DisponibilidadeContatoModel();
        model.setTelefoneDisponivel(disponibilidade.telefoneDisponivel());
        model.setEmailDisponivel(disponibilidade.emailDisponivel());
        return model;
    }

    @GetMapping("/{clienteId}")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Buscar cliente por ID")
    public ClienteModel buscar(@PathVariable UUID clienteId) {
        return clienteAssembler.toModel(consultarClienteUseCase.buscarOuFalhar(clienteId));
    }

    @PutMapping("/{clienteId}")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Atualizar cliente")
    public ClienteModel atualizar(
            @PathVariable UUID clienteId,
            @RequestBody @Valid ClienteAtualizacaoInput input) {
        var cliente = atualizarClienteUseCase.executar(clienteId, new AtualizarClienteUseCase.Input(
            input.getNome(),
            input.getDataNascimento(),
            input.getSexo(),
            input.getTelefone(),
            input.getEmail(),
            input.getEndereco(),
            input.getAlergias(),
            input.getObservacoes(),
            input.getAtivo()
        ));
        return clienteAssembler.toModel(cliente);
    }
}
