package br.com.farmacia.application.compra.usecase;

import br.com.farmacia.application.estoque.usecase.RegistrarEntradaEstoqueUseCase;
import br.com.farmacia.domain.compra.entity.DivergenciaConferencia;
import br.com.farmacia.domain.compra.entity.NotaFiscalEntrada;
import br.com.farmacia.domain.compra.entity.PedidoCompra;
import br.com.farmacia.domain.compra.enums.StatusNota;
import br.com.farmacia.domain.compra.exception.ChaveDuplicadaException;
import br.com.farmacia.domain.compra.exception.ChaveInvalidaException;
import br.com.farmacia.domain.compra.exception.FornecedorNaoEncontradoException;
import br.com.farmacia.domain.compra.exception.ItensObrigatoriosException;
import br.com.farmacia.domain.compra.exception.NotaInvalidaException;
import br.com.farmacia.domain.compra.exception.PedidoFornecedorIncompativelException;
import br.com.farmacia.domain.compra.exception.PedidoNaoEncontradoException;
import br.com.farmacia.domain.compra.repository.FornecedorRepository;
import br.com.farmacia.domain.compra.repository.NotaFiscalEntradaRepository;
import br.com.farmacia.domain.compra.repository.PedidoCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registra NF-e de entrada, processa estoque e opcionalmente confere com pedido de compra.
 */
@Service
@RequiredArgsConstructor
public class RegistrarNotaFiscalEntradaUseCase {

    private final FornecedorRepository fornecedorRepository;
    private final NotaFiscalEntradaRepository notaFiscalRepository;
    private final PedidoCompraRepository pedidoCompraRepository;
    private final RegistrarEntradaEstoqueUseCase registrarEntradaEstoqueUseCase;
    private final ConferirNotaComPedidoUseCase conferirNotaComPedidoUseCase;

    @Transactional
    public Output executar(Input input) {
        validarCabecalho(input);

        var fornecedor = fornecedorRepository.findById(input.fornecedorId())
            .orElseThrow(() -> new FornecedorNaoEncontradoException(input.fornecedorId()));

        PedidoCompra pedido = null;
        if (input.pedidoCompraId() != null) {
            pedido = pedidoCompraRepository.findById(input.pedidoCompraId())
                .orElseThrow(() -> new PedidoNaoEncontradoException(input.pedidoCompraId()));
            if (!fornecedor.getId().equals(pedido.getFornecedorId())) {
                throw new PedidoFornecedorIncompativelException();
            }
        }

        String chave = normalizarChave(input.chaveAcesso());
        if (notaFiscalRepository.findByChaveAcesso(chave).isPresent()) {
            throw new ChaveDuplicadaException(chave);
        }

        if (input.itens() == null || input.itens().isEmpty()) {
            throw new ItensObrigatoriosException();
        }

        BigDecimal valorCalculado = input.itens().stream()
            .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        NotaFiscalEntrada nota = NotaFiscalEntrada.builder()
            .fornecedorId(fornecedor.getId())
            .fornecedorNome(fornecedor.getRazaoSocial())
            .pedidoCompraId(input.pedidoCompraId())
            .numeroNota(input.numeroNota().trim())
            .serie(input.serie() != null ? input.serie().trim() : null)
            .chaveAcesso(chave)
            .dataEmissao(input.dataEmissao())
            .dataEntrada(input.dataEntrada() != null ? input.dataEntrada() : LocalDate.now())
            .valorTotal(input.valorTotal() != null ? input.valorTotal() : valorCalculado)
            .status(StatusNota.RECEBIDA)
            .build();

        nota = notaFiscalRepository.save(nota);

        List<RegistrarEntradaEstoqueUseCase.Output> entradas = new ArrayList<>();
        for (ItemInput item : input.itens()) {
            entradas.add(registrarEntradaEstoqueUseCase.executar(
                new RegistrarEntradaEstoqueUseCase.Input(
                    item.medicamentoId(),
                    item.numeroLote().trim(),
                    item.dataValidade(),
                    item.dataFabricacao(),
                    item.quantidade(),
                    item.precoUnitario(),
                    null,
                    null,
                    "NF-e %s — fornecedor %s".formatted(nota.getNumeroNota(), fornecedor.getRazaoSocial()),
                    nota.getId(),
                    nota.getId()
                )));
        }

        List<DivergenciaConferencia> divergencias = List.of();
        StatusNota statusConferencia = nota.getStatus();
        PedidoCompra pedidoAtualizado = pedido;

        if (pedido != null) {
            var itensConferencia = input.itens().stream()
                .map(i -> new ConferirNotaComPedidoUseCase.ItemNota(
                    i.medicamentoId(), i.quantidade(), i.precoUnitario()))
                .toList();
            var conferencia = conferirNotaComPedidoUseCase.executar(pedido, itensConferencia);
            divergencias = conferencia.divergencias();
            statusConferencia = conferencia.statusNota();
            pedidoAtualizado = conferencia.pedido();
        }

        nota.finalizarConferencia(entradas.size(), statusConferencia);
        nota = notaFiscalRepository.save(nota);

        return new Output(nota, entradas.size(), pedidoAtualizado, divergencias, statusConferencia);
    }

    private void validarCabecalho(Input input) {
        if (input.numeroNota() == null || input.numeroNota().isBlank()) {
            throw new NotaInvalidaException("Número da nota é obrigatório");
        }
        String chave = normalizarChave(input.chaveAcesso());
        if (chave.length() != 44) {
            throw new ChaveInvalidaException();
        }
    }

    static String normalizarChave(String chave) {
        return chave != null ? chave.replaceAll("\\D", "") : "";
    }

    public record ItemInput(
        UUID medicamentoId,
        String numeroLote,
        LocalDate dataValidade,
        LocalDate dataFabricacao,
        int quantidade,
        BigDecimal precoUnitario
    ) {}

    public record Input(
        UUID fornecedorId,
        UUID pedidoCompraId,
        String numeroNota,
        String serie,
        String chaveAcesso,
        LocalDate dataEmissao,
        LocalDate dataEntrada,
        BigDecimal valorTotal,
        List<ItemInput> itens
    ) {}

    public record Output(
        NotaFiscalEntrada nota,
        int itensProcessados,
        PedidoCompra pedido,
        List<DivergenciaConferencia> divergencias,
        StatusNota statusConferencia
    ) {}
}
