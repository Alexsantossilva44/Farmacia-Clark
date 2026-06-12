package br.com.farmacia.api.v1.assembler;

import br.com.farmacia.api.v1.model.output.*;
import br.com.farmacia.application.compra.usecase.RegistrarNotaFiscalEntradaUseCase;
import br.com.farmacia.domain.compra.entity.DivergenciaConferencia;
import br.com.farmacia.domain.compra.entity.Fornecedor;
import br.com.farmacia.domain.compra.entity.ItemPedidoCompra;
import br.com.farmacia.domain.compra.entity.NotaFiscalEntrada;
import br.com.farmacia.domain.compra.entity.PedidoCompra;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompraAssembler {

    public FornecedorModel toModel(Fornecedor f) {
        var m = new FornecedorModel();
        m.setId(f.getId());
        m.setRazaoSocial(f.getRazaoSocial());
        m.setNomeFantasia(f.getNomeFantasia());
        m.setCnpj(f.getCnpj());
        m.setAtivo(f.getAtivo());
        return m;
    }

    public List<FornecedorModel> toFornecedorCollection(List<Fornecedor> fornecedores) {
        return fornecedores.stream().map(this::toModel).toList();
    }

    public NotaFiscalEntradaModel toModel(NotaFiscalEntrada n) {
        var m = new NotaFiscalEntradaModel();
        m.setId(n.getId());
        m.setPedidoCompraId(n.getPedidoCompraId());
        m.setFornecedorId(n.getFornecedorId());
        m.setFornecedorNome(n.getFornecedorNome());
        m.setNumeroNota(n.getNumeroNota());
        m.setSerie(n.getSerie());
        m.setChaveAcesso(n.getChaveAcesso());
        m.setDataEmissao(n.getDataEmissao());
        m.setDataEntrada(n.getDataEntrada());
        m.setValorTotal(n.getValorTotal());
        m.setStatus(n.getStatus() != null ? n.getStatus().name() : null);
        m.setCreatedAt(n.getCreatedAt());
        m.setQuantidadeItens(n.getQuantidadeItens());
        return m;
    }

    public List<NotaFiscalEntradaModel> toNotaCollection(List<NotaFiscalEntrada> notas) {
        return notas.stream().map(this::toModel).toList();
    }

    public PedidoCompraModel toModel(PedidoCompra p) {
        var m = new PedidoCompraModel();
        m.setId(p.getId());
        m.setFornecedorId(p.getFornecedorId());
        m.setFornecedorNome(p.getFornecedorNome());
        m.setDataPedido(p.getDataPedido());
        m.setDataEntregaPrevista(p.getDataEntregaPrevista());
        m.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        m.setValorTotal(p.getValorTotal());
        m.setObservacao(p.getObservacao());
        m.setCreatedAt(p.getCreatedAt());
        m.setQuantidadePendente(p.getItens() != null ? p.quantidadePendenteTotal() : null);
        if (p.getItens() != null && !p.getItens().isEmpty()) {
            m.setItens(p.getItens().stream().map(this::toModel).toList());
        }
        return m;
    }

    public ItemPedidoCompraModel toModel(ItemPedidoCompra i) {
        var m = new ItemPedidoCompraModel();
        m.setId(i.getId());
        m.setMedicamentoId(i.getMedicamentoId());
        m.setMedicamentoNome(i.getMedicamentoNome());
        m.setQuantidadeSolicitada(i.getQuantidadeSolicitada());
        m.setQuantidadeRecebida(i.getQuantidadeRecebida());
        m.setQuantidadePendente(i.quantidadePendente());
        m.setPrecoUnitario(i.getPrecoUnitario());
        return m;
    }

    public List<PedidoCompraModel> toPedidoCollection(List<PedidoCompra> pedidos) {
        return pedidos.stream().map(this::toModel).toList();
    }

    public DivergenciaConferenciaModel toModel(DivergenciaConferencia d) {
        var m = new DivergenciaConferenciaModel();
        m.setTipo(d.getTipo() != null ? d.getTipo().name() : null);
        m.setMedicamentoId(d.getMedicamentoId());
        m.setMedicamentoNome(d.getMedicamentoNome());
        m.setQuantidadeEsperada(d.getQuantidadeEsperada());
        m.setQuantidadeRecebida(d.getQuantidadeRecebida());
        m.setPrecoEsperado(d.getPrecoEsperado());
        m.setPrecoRecebido(d.getPrecoRecebido());
        m.setMensagem(d.getMensagem());
        return m;
    }

    public NotaFiscalEntradaResultModel toResultModel(RegistrarNotaFiscalEntradaUseCase.Output output) {
        var result = new NotaFiscalEntradaResultModel();
        result.setNota(toModel(output.nota()));

        if (output.pedido() != null) {
            var conf = new ConferenciaNotaModel();
            conf.setConferida(output.divergencias().isEmpty());
            conf.setStatusNota(output.statusConferencia() != null ? output.statusConferencia().name() : null);
            conf.setPedidoCompraId(output.pedido().getId());
            conf.setStatusPedido(output.pedido().getStatus() != null ? output.pedido().getStatus().name() : null);
            conf.setDivergencias(output.divergencias().stream().map(this::toModel).toList());
            result.setConferencia(conf);
        }
        return result;
    }
}
