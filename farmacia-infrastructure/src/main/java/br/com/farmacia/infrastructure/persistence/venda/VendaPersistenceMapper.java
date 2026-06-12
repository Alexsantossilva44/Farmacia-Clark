package br.com.farmacia.infrastructure.persistence.venda;

import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.financeiro.entity.Caixa;
import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.funcionario.entity.Funcionario;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.venda.entity.ItemVenda;
import br.com.farmacia.domain.venda.entity.Pagamento;
import br.com.farmacia.domain.venda.entity.Venda;
import br.com.farmacia.domain.venda.enums.StatusPagamento;
import br.com.farmacia.domain.venda.enums.StatusVenda;
import br.com.farmacia.domain.venda.enums.TipoAtendimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mapeamento manual entre o agregado {@link Venda} (domínio) e
 * {@link VendaJpaEntity}, incluindo itens e pagamentos.
 *
 * @author Alex Silva e Claude
 */
public final class VendaPersistenceMapper {

    private VendaPersistenceMapper() {
    }

    public static VendaJpaEntity toJpa(Venda v) {
        VendaJpaEntity entity = VendaJpaEntity.builder()
            .id(v.getId())
            .pdvId(v.getPdv() != null ? v.getPdv().getId() : null)
            .caixaId(v.getCaixa() != null ? v.getCaixa().getId() : null)
            .funcionarioId(resolverFuncionarioId(v))
            .clienteId(v.getCliente() != null ? v.getCliente().getId() : null)
            .receitaId(v.getReceita() != null ? v.getReceita().getId() : null)
            .dataHora(v.getDataHora() != null ? v.getDataHora() : LocalDateTime.now())
            .status(v.getStatus() != null ? v.getStatus() : StatusVenda.ABERTA)
            .tipoAtendimento(v.getTipoAtendimento() != null
                ? v.getTipoAtendimento() : TipoAtendimento.BALCAO)
            .subtotal(v.getSubtotal() != null ? v.getSubtotal() : BigDecimal.ZERO)
            .desconto(v.getDesconto() != null ? v.getDesconto() : BigDecimal.ZERO)
            .total(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
            .numeroCupom(v.getNumeroCupom())
            .observacao(v.getObservacao())
            .itens(new ArrayList<>())
            .pagamentos(new ArrayList<>())
            .build();

        if (v.getItens() != null) {
            for (ItemVenda item : v.getItens()) {
                entity.getItens().add(toItemJpa(item, entity));
            }
        }
        if (v.getPagamentos() != null) {
            for (Pagamento pagamento : v.getPagamentos()) {
                entity.getPagamentos().add(toPagamentoJpa(pagamento, entity));
            }
        }
        return entity;
    }

    private static ItemVendaJpaEntity toItemJpa(ItemVenda i, VendaJpaEntity venda) {
        return ItemVendaJpaEntity.builder()
            .id(i.getId() != null ? i.getId() : UUID.randomUUID())
            .venda(venda)
            .medicamentoId(i.getMedicamento() != null ? i.getMedicamento().getId() : null)
            .loteId(i.getLote() != null ? i.getLote().getId() : null)
            .quantidade(i.getQuantidade())
            .precoUnitario(i.getPrecoUnitario())
            .desconto(i.getDesconto() != null ? i.getDesconto() : BigDecimal.ZERO)
            .subtotal(i.getSubtotal())
            .build();
    }

    private static PagamentoJpaEntity toPagamentoJpa(Pagamento p, VendaJpaEntity venda) {
        return PagamentoJpaEntity.builder()
            .id(p.getId() != null ? p.getId() : UUID.randomUUID())
            .venda(venda)
            .forma(p.getForma())
            .valor(p.getValor())
            .troco(p.getTroco() != null ? p.getTroco() : BigDecimal.ZERO)
            .nsu(p.getNsu())
            .autorizacao(p.getAutorizacao())
            .status(p.getStatus() != null ? p.getStatus() : StatusPagamento.PENDENTE)
            .dataHora(p.getDataHora() != null ? p.getDataHora() : LocalDateTime.now())
            .build();
    }

    public static Venda toDomain(VendaJpaEntity e) {
        Venda venda = Venda.builder()
            .id(e.getId())
            .pdv(e.getPdvId() != null ? PDV.builder().id(e.getPdvId()).build() : null)
            .caixa(e.getCaixaId() != null ? Caixa.builder().id(e.getCaixaId()).build() : null)
            .funcionario(e.getFuncionarioId() != null
                ? Funcionario.builder().id(e.getFuncionarioId()).build() : null)
            .cliente(e.getClienteId() != null
                ? Cliente.builder().id(e.getClienteId()).build() : null)
            .receita(e.getReceitaId() != null
                ? Receita.builder().id(e.getReceitaId()).build() : null)
            .dataHora(e.getDataHora())
            .numeroCupom(e.getNumeroCupom())
            .status(e.getStatus())
            .tipoAtendimento(e.getTipoAtendimento())
            .subtotal(e.getSubtotal())
            .desconto(e.getDesconto())
            .total(e.getTotal())
            .observacao(e.getObservacao())
            .itens(new ArrayList<>())
            .pagamentos(new ArrayList<>())
            .build();

        if (e.getItens() != null) {
            List<ItemVenda> itens = e.getItens().stream()
                .map(i -> toItemDomain(i, venda))
                .toList();
            venda.getItens().addAll(itens);
        }
        if (e.getPagamentos() != null) {
            List<Pagamento> pagamentos = e.getPagamentos().stream()
                .map(p -> toPagamentoDomain(p, venda))
                .toList();
            venda.getPagamentos().addAll(pagamentos);
        }
        return venda;
    }

    private static ItemVenda toItemDomain(ItemVendaJpaEntity i, Venda venda) {
        return ItemVenda.builder()
            .id(i.getId())
            .venda(venda)
            .medicamento(i.getMedicamentoId() != null
                ? Medicamento.builder().id(i.getMedicamentoId()).build() : null)
            .lote(i.getLoteId() != null ? Lote.builder().id(i.getLoteId()).build() : null)
            .quantidade(i.getQuantidade())
            .precoUnitario(i.getPrecoUnitario())
            .desconto(i.getDesconto())
            .subtotal(i.getSubtotal())
            .build();
    }

    private static Pagamento toPagamentoDomain(PagamentoJpaEntity p, Venda venda) {
        return Pagamento.builder()
            .id(p.getId())
            .venda(venda)
            .forma(p.getForma())
            .valor(p.getValor())
            .troco(p.getTroco())
            .nsu(p.getNsu())
            .autorizacao(p.getAutorizacao())
            .status(p.getStatus())
            .dataHora(p.getDataHora())
            .build();
    }

    private static UUID resolverFuncionarioId(Venda v) {
        if (v.getFuncionario() != null && v.getFuncionario().getId() != null) {
            return v.getFuncionario().getId();
        }
        if (v.getCaixa() != null && v.getCaixa().getFuncionario() != null) {
            return v.getCaixa().getFuncionario().getId();
        }
        return null;
    }
}
