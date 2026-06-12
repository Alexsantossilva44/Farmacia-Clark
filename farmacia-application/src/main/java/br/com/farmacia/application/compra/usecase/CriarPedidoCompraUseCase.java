package br.com.farmacia.application.compra.usecase;

import br.com.farmacia.domain.compra.entity.ItemPedidoCompra;
import br.com.farmacia.domain.compra.entity.PedidoCompra;
import br.com.farmacia.domain.compra.enums.StatusPedido;
import br.com.farmacia.domain.compra.exception.FornecedorNaoEncontradoException;
import br.com.farmacia.domain.compra.exception.ItensObrigatoriosException;
import br.com.farmacia.domain.compra.repository.FornecedorRepository;
import br.com.farmacia.domain.compra.repository.PedidoCompraRepository;
import br.com.farmacia.domain.estoque.exception.QuantidadeInvalidaException;
import br.com.farmacia.domain.medicamento.exception.MedicamentoNaoEncontradoException;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CriarPedidoCompraUseCase {

    private final PedidoCompraRepository pedidoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final MedicamentoRepository medicamentoRepository;

    @Transactional
    public PedidoCompra executar(Input input) {
        var fornecedor = fornecedorRepository.findById(input.fornecedorId())
            .orElseThrow(() -> new FornecedorNaoEncontradoException(input.fornecedorId()));

        if (input.itens() == null || input.itens().isEmpty()) {
            throw new ItensObrigatoriosException();
        }

        List<ItemPedidoCompra> itens = input.itens().stream().map(i -> {
            var med = medicamentoRepository.findById(i.medicamentoId())
                .orElseThrow(() -> new MedicamentoNaoEncontradoException(i.medicamentoId()));
            if (i.quantidadeSolicitada() <= 0) {
                throw new QuantidadeInvalidaException();
            }
            return ItemPedidoCompra.builder()
                .medicamentoId(med.getId())
                .medicamentoNome(med.getNomeComercial())
                .quantidadeSolicitada(i.quantidadeSolicitada())
                .quantidadeRecebida(0)
                .precoUnitario(i.precoUnitario())
                .build();
        }).toList();

        BigDecimal valorTotal = itens.stream()
            .map(i -> {
                BigDecimal preco = i.getPrecoUnitario() != null ? i.getPrecoUnitario() : BigDecimal.ZERO;
                return preco.multiply(BigDecimal.valueOf(i.getQuantidadeSolicitada()));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return pedidoRepository.save(PedidoCompra.builder()
            .fornecedorId(fornecedor.getId())
            .fornecedorNome(fornecedor.getRazaoSocial())
            .dataPedido(input.dataPedido() != null ? input.dataPedido() : LocalDate.now())
            .dataEntregaPrevista(input.dataEntregaPrevista())
            .status(StatusPedido.RASCUNHO)
            .valorTotal(valorTotal)
            .observacao(input.observacao())
            .itens(itens)
            .build());
    }

    public record ItemInput(UUID medicamentoId, int quantidadeSolicitada, BigDecimal precoUnitario) {}

    public record Input(
        UUID fornecedorId,
        LocalDate dataPedido,
        LocalDate dataEntregaPrevista,
        String observacao,
        List<ItemInput> itens
    ) {}
}
