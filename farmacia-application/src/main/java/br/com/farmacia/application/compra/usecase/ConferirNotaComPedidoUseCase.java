package br.com.farmacia.application.compra.usecase;

import br.com.farmacia.domain.compra.entity.DivergenciaConferencia;
import br.com.farmacia.domain.compra.entity.ItemPedidoCompra;
import br.com.farmacia.domain.compra.entity.PedidoCompra;
import br.com.farmacia.domain.compra.enums.StatusNota;
import br.com.farmacia.domain.compra.enums.StatusPedido;
import br.com.farmacia.domain.compra.enums.TipoDivergenciaConferencia;
import br.com.farmacia.domain.compra.exception.PedidoInvalidoException;
import br.com.farmacia.domain.compra.repository.PedidoCompraRepository;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // H-07: garante consistência na atualização do pedido

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Compara itens da NF-e com o pedido de compra vinculado e atualiza quantidades recebidas.
 */
@Service
@RequiredArgsConstructor
public class ConferirNotaComPedidoUseCase {

    private final PedidoCompraRepository pedidoCompraRepository;
    private final MedicamentoRepository medicamentoRepository;

    @Transactional // H-07: pedido e divergências devem ser persistidos na mesma transação
    public Result executar(PedidoCompra pedido, List<ItemNota> itensNota) {
        validarPedidoRecebivel(pedido);

        List<DivergenciaConferencia> divergencias = new ArrayList<>();
        Map<UUID, ItemPedidoCompra> itensPedido = pedido.getItens().stream()
            .collect(Collectors.toMap(ItemPedidoCompra::getMedicamentoId, Function.identity()));

        for (ItemNota itemNota : itensNota) {
            String nomeMed = medicamentoRepository.findById(itemNota.medicamentoId())
                .map(m -> m.getNomeComercial())
                .orElse("Medicamento " + itemNota.medicamentoId());

            ItemPedidoCompra itemPedido = itensPedido.get(itemNota.medicamentoId());
            if (itemPedido == null) {
                divergencias.add(DivergenciaConferencia.builder()
                    .tipo(TipoDivergenciaConferencia.ITEM_NAO_PEDIDO)
                    .medicamentoId(itemNota.medicamentoId())
                    .medicamentoNome(nomeMed)
                    .quantidadeRecebida(itemNota.quantidade())
                    .mensagem("Produto não consta no pedido de compra")
                    .build());
                continue;
            }

            int pendente = itemPedido.quantidadePendente();
            if (itemNota.quantidade() > pendente) {
                divergencias.add(DivergenciaConferencia.builder()
                    .tipo(TipoDivergenciaConferencia.QUANTIDADE_EXCEDENTE)
                    .medicamentoId(itemNota.medicamentoId())
                    .medicamentoNome(nomeMed)
                    .quantidadeEsperada(pendente)
                    .quantidadeRecebida(itemNota.quantidade())
                    .mensagem("Quantidade na NF (%d) excede pendente no pedido (%d)"
                        .formatted(itemNota.quantidade(), pendente))
                    .build());
            }

            if (itemPedido.getPrecoUnitario() != null
                    && itemNota.precoUnitario() != null
                    && itemPedido.getPrecoUnitario().compareTo(itemNota.precoUnitario()) != 0) {
                divergencias.add(DivergenciaConferencia.builder()
                    .tipo(TipoDivergenciaConferencia.PRECO_DIFERENTE)
                    .medicamentoId(itemNota.medicamentoId())
                    .medicamentoNome(nomeMed)
                    .precoEsperado(itemPedido.getPrecoUnitario())
                    .precoRecebido(itemNota.precoUnitario())
                    .mensagem("Preço unitário diverge do pedido")
                    .build());
            }

            itemPedido.registrarRecebimento(itemNota.quantidade());
        }

        // H-06: detecta itens do pedido que não estão presentes na NF-e (entrega parcial do fornecedor)
        Set<UUID> medicamentosNaNota = itensNota.stream()
            .map(ItemNota::medicamentoId)
            .collect(java.util.stream.Collectors.toSet());
        for (ItemPedidoCompra itemPedido : itensPedido.values()) {
            if (!medicamentosNaNota.contains(itemPedido.getMedicamentoId())
                    && itemPedido.quantidadePendente() > 0) {
                String nomeMed = medicamentoRepository.findById(itemPedido.getMedicamentoId())
                    .map(m -> m.getNomeComercial())
                    .orElse("Medicamento " + itemPedido.getMedicamentoId());
                divergencias.add(DivergenciaConferencia.builder()
                    .tipo(TipoDivergenciaConferencia.ITEM_AUSENTE_NA_NOTA)
                    .medicamentoId(itemPedido.getMedicamentoId())
                    .medicamentoNome(nomeMed)
                    .quantidadeEsperada(itemPedido.quantidadePendente())
                    .quantidadeRecebida(0)
                    .mensagem("Item do pedido ausente na NF-e: esperado %d unidade(s)"
                        .formatted(itemPedido.quantidadePendente()))
                    .build());
            }
        }

        pedido.atualizarStatusRecebimento();
        PedidoCompra pedidoAtualizado = pedidoCompraRepository.save(pedido);

        StatusNota statusNota = divergencias.isEmpty() ? StatusNota.CONFERIDA : StatusNota.DIVERGENCIA;
        return new Result(pedidoAtualizado, statusNota, divergencias);
    }

    private void validarPedidoRecebivel(PedidoCompra pedido) {
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new PedidoInvalidoException("Pedido cancelado não pode receber NF-e");
        }
        if (pedido.getStatus() == StatusPedido.RASCUNHO) {
            throw new PedidoInvalidoException("Confirme o pedido antes de vincular a NF-e");
        }
        if (pedido.getStatus() == StatusPedido.RECEBIDO) {
            throw new PedidoInvalidoException("Pedido já totalmente recebido");
        }
    }

    public record ItemNota(UUID medicamentoId, int quantidade, BigDecimal precoUnitario) {}

    public record Result(
        PedidoCompra pedido,
        StatusNota statusNota,
        List<DivergenciaConferencia> divergencias
    ) {
        public boolean conferida() {
            return divergencias.isEmpty();
        }
    }
}
