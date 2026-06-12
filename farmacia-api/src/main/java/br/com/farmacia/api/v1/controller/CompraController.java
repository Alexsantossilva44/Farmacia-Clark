package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.CompraAssembler;
import br.com.farmacia.api.v1.model.input.NotaFiscalEntradaInput;
import br.com.farmacia.api.v1.model.input.PedidoCompraInput;
import br.com.farmacia.api.v1.model.output.NotaFiscalEntradaModel;
import br.com.farmacia.api.v1.model.output.NotaFiscalEntradaResultModel;
import br.com.farmacia.api.v1.model.output.PedidoCompraModel;
import br.com.farmacia.application.compra.usecase.*;
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
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/compras", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Compras", description = "Pedidos, NF-e de entrada e conferência")
@SecurityRequirement(name = "bearerAuth")
public class CompraController {

    private final ListarNotasFiscaisEntradaUseCase listarNotasUseCase;
    private final RegistrarNotaFiscalEntradaUseCase registrarNotaUseCase;
    private final ListarPedidosCompraUseCase listarPedidosUseCase;
    private final ConsultarPedidoCompraUseCase consultarPedidoUseCase;
    private final CriarPedidoCompraUseCase criarPedidoUseCase;
    private final ConfirmarPedidoCompraUseCase confirmarPedidoUseCase;
    private final CompraAssembler compraAssembler;

    @GetMapping("/pedidos")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Listar pedidos de compra")
    public List<PedidoCompraModel> listarPedidos() {
        return compraAssembler.toPedidoCollection(listarPedidosUseCase.executar());
    }

    @GetMapping("/pedidos/{id}")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Consultar pedido de compra com itens")
    public PedidoCompraModel consultarPedido(@PathVariable UUID id) {
        return compraAssembler.toModel(consultarPedidoUseCase.executar(id));
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Criar pedido de compra (rascunho)")
    public PedidoCompraModel criarPedido(@RequestBody @Valid PedidoCompraInput input) {
        var itens = input.getItens().stream()
            .map(i -> new CriarPedidoCompraUseCase.ItemInput(
                i.getMedicamentoId(),
                i.getQuantidadeSolicitada(),
                i.getPrecoUnitario()))
            .toList();
        return compraAssembler.toModel(criarPedidoUseCase.executar(
            new CriarPedidoCompraUseCase.Input(
                input.getFornecedorId(),
                input.getDataPedido(),
                input.getDataEntregaPrevista(),
                input.getObservacao(),
                itens)));
    }

    @PutMapping("/pedidos/{id}/confirmar")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Confirmar pedido para recebimento de NF-e")
    public PedidoCompraModel confirmarPedido(@PathVariable UUID id) {
        return compraAssembler.toModel(confirmarPedidoUseCase.executar(id));
    }

    @GetMapping("/notas")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Listar notas fiscais de entrada")
    public List<NotaFiscalEntradaModel> listarNotas() {
        return compraAssembler.toNotaCollection(listarNotasUseCase.executar());
    }

    @PostMapping("/notas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Registrar NF-e, dar entrada no estoque e conferir com pedido")
    public NotaFiscalEntradaResultModel registrarNota(@RequestBody @Valid NotaFiscalEntradaInput input) {
        var itens = input.getItens().stream()
            .map(i -> new RegistrarNotaFiscalEntradaUseCase.ItemInput(
                i.getMedicamentoId(),
                i.getNumeroLote(),
                i.getDataValidade(),
                i.getDataFabricacao(),
                i.getQuantidade(),
                i.getPrecoUnitario()))
            .toList();

        var output = registrarNotaUseCase.executar(new RegistrarNotaFiscalEntradaUseCase.Input(
            input.getFornecedorId(),
            input.getPedidoCompraId(),
            input.getNumeroNota(),
            input.getSerie(),
            input.getChaveAcesso(),
            input.getDataEmissao(),
            input.getDataEntrada(),
            input.getValorTotal(),
            itens));

        return compraAssembler.toResultModel(output);
    }
}
