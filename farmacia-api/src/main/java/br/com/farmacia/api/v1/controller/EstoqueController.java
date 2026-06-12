package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.assembler.EstoqueAssembler;
import br.com.farmacia.api.v1.model.AlertaEstoqueModel;
import br.com.farmacia.api.v1.model.ItemEstoqueModel;
import br.com.farmacia.api.v1.model.LoteModel;
import br.com.farmacia.api.v1.model.input.AtualizarItemEstoqueInput;
import br.com.farmacia.api.v1.model.input.AjusteSaldoInput;
import br.com.farmacia.api.v1.model.input.EntradaEstoqueInput;
import br.com.farmacia.api.v1.model.output.AjusteSaldoModel;
import br.com.farmacia.api.v1.model.output.EntradaEstoqueModel;
import br.com.farmacia.api.v1.model.output.DisponivelVendaModel;
import br.com.farmacia.api.v1.model.output.MovimentacaoEstoqueModel;
import br.com.farmacia.domain.estoque.enums.TipoMovimentacao;
import br.com.farmacia.application.estoque.usecase.AtualizarItemEstoqueUseCase;
import br.com.farmacia.application.estoque.usecase.ConsultarEstoqueUseCase;
import br.com.farmacia.application.estoque.usecase.ListarEstoqueUseCase;
import br.com.farmacia.application.estoque.usecase.ListarMovimentacoesEstoqueUseCase;
import br.com.farmacia.application.estoque.usecase.RegistrarAjusteSaldoUseCase;
import br.com.farmacia.application.estoque.usecase.RegistrarEntradaEstoqueUseCase;
import br.com.farmacia.domain.estoque.enums.TipoAlerta;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/estoque", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "Saldos, lotes FEFO, entradas e alertas")
@SecurityRequirement(name = "bearerAuth")
public class EstoqueController {

    private final ConsultarEstoqueUseCase consultarEstoqueUseCase;
    private final ListarEstoqueUseCase listarEstoqueUseCase;
    private final ListarMovimentacoesEstoqueUseCase listarMovimentacoesUseCase;
    private final RegistrarEntradaEstoqueUseCase registrarEntradaEstoqueUseCase;
    private final RegistrarAjusteSaldoUseCase registrarAjusteSaldoUseCase;
    private final AtualizarItemEstoqueUseCase atualizarItemEstoqueUseCase;
    private final EstoqueAssembler estoqueAssembler;

    @GetMapping("/itens")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Listar medicamentos ativos com saldo consolidado (paginado)")
    public Page<ItemEstoqueModel> listarItens(
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 20, sort = "medicamentoNome", direction = Sort.Direction.ASC)
            Pageable pageable) {
        var pagina = listarEstoqueUseCase.executar(pageable, busca);
        var disponivelVenda = consultarEstoqueUseCase.mapaDisponivelVenda();
        return estoqueAssembler.toItemPage(pagina, disponivelVenda);
    }

    @GetMapping("/disponivel-venda")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Mapa compacto de saldo dispensável por medicamento (PDV)")
    public List<DisponivelVendaModel> mapaDisponivelVenda() {
        return consultarEstoqueUseCase.mapaDisponivelVenda().entrySet().stream()
            .map(e -> new DisponivelVendaModel(e.getKey(), e.getValue()))
            .toList();
    }

    @GetMapping("/movimentacoes")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Histórico de movimentações de estoque")
    public Page<MovimentacaoEstoqueModel> listarMovimentacoes(
            @RequestParam(required = false) UUID medicamentoId,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return listarMovimentacoesUseCase.executar(medicamentoId, tipo, pageable)
            .map(estoqueAssembler::toModel);
    }

    @GetMapping("/medicamentos/{medicamentoId}")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Saldo consolidado por medicamento")
    public ItemEstoqueModel saldo(@PathVariable UUID medicamentoId) {
        return estoqueAssembler.toModel(consultarEstoqueUseCase.buscarSaldoPorMedicamento(medicamentoId));
    }

    @GetMapping("/medicamentos/{medicamentoId}/lotes")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Lotes disponíveis ordenados por FEFO")
    public List<LoteModel> lotesFefo(@PathVariable UUID medicamentoId) {
        return estoqueAssembler.toLoteCollection(
            consultarEstoqueUseCase.listarLotesFefo(medicamentoId));
    }

    @GetMapping("/medicamentos/{medicamentoId}/lotes/ajuste")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Todos os lotes do medicamento para ajuste de inventário")
    public List<LoteModel> lotesParaAjuste(@PathVariable UUID medicamentoId) {
        return estoqueAssembler.toLoteCollection(
            consultarEstoqueUseCase.listarLotesParaAjuste(medicamentoId));
    }

    @PostMapping("/entrada")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Registrar entrada de estoque (novo lote ou incremento)")
    public EntradaEstoqueModel registrarEntrada(@RequestBody @Valid EntradaEstoqueInput input) {
        var output = registrarEntradaEstoqueUseCase.executar(new RegistrarEntradaEstoqueUseCase.Input(
            input.getMedicamentoId(),
            input.getNumeroLote(),
            input.getDataValidade(),
            input.getDataFabricacao(),
            input.getQuantidade(),
            input.getPrecoCusto(),
            input.getQuantidadeMinima(),
            input.getQuantidadeMaxima(),
            input.getObservacao(),
            null,
            null
        ));
        var model = new EntradaEstoqueModel();
        model.setItemEstoque(estoqueAssembler.toModel(output.itemEstoque()));
        model.setLote(estoqueAssembler.toModel(output.lote()));
        return model;
    }

    @PostMapping("/ajuste")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Ajuste manual de saldo (inventário, perda ou correção)")
    public AjusteSaldoModel registrarAjuste(@RequestBody @Valid AjusteSaldoInput input) {
        var output = registrarAjusteSaldoUseCase.executar(new RegistrarAjusteSaldoUseCase.Input(
            input.getMedicamentoId(),
            input.getLoteId(),
            input.getTipo(),
            input.getQuantidade(),
            input.getMotivo()
        ));
        var model = new AjusteSaldoModel();
        model.setItemEstoque(estoqueAssembler.toModel(output.itemEstoque()));
        model.setLote(estoqueAssembler.toModel(output.lote()));
        return model;
    }

    @PutMapping("/medicamentos/{medicamentoId}/parametros")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Atualizar quantidade mínima e máxima do estoque")
    public ItemEstoqueModel atualizarParametros(
            @PathVariable UUID medicamentoId,
            @RequestBody AtualizarItemEstoqueInput input) {
        return estoqueAssembler.toModel(atualizarItemEstoqueUseCase.executar(
            medicamentoId,
            new AtualizarItemEstoqueUseCase.Input(input.getQuantidadeMinima(), input.getQuantidadeMaxima())
        ));
    }

    @GetMapping("/abaixo-minimo")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Itens com saldo abaixo do mínimo")
    public List<ItemEstoqueModel> abaixoMinimo() {
        return estoqueAssembler.toItemCollection(consultarEstoqueUseCase.listarAbaixoDoMinimo());
    }

    @GetMapping("/zerados")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Itens com estoque zerado")
    public List<ItemEstoqueModel> zerados() {
        return estoqueAssembler.toItemCollection(consultarEstoqueUseCase.listarEstoqueZerado());
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Alertas de estoque abertos")
    public List<AlertaEstoqueModel> alertas(@RequestParam(required = false) TipoAlerta tipo) {
        return estoqueAssembler.toAlertaCollection(consultarEstoqueUseCase.listarAlertasAbertos(tipo));
    }
}
