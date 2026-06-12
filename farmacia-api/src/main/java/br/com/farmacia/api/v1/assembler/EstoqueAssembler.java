package br.com.farmacia.api.v1.assembler;

import br.com.farmacia.api.v1.model.AlertaEstoqueModel;
import br.com.farmacia.api.v1.model.ItemEstoqueModel;
import br.com.farmacia.api.v1.model.LoteModel;
import br.com.farmacia.api.v1.model.output.MovimentacaoEstoqueModel;
import br.com.farmacia.domain.estoque.entity.AlertaEstoque;
import br.com.farmacia.domain.estoque.entity.ItemEstoque;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.entity.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class EstoqueAssembler {

    public ItemEstoqueModel toModel(ItemEstoque item) {
        return toModel(item, null);
    }

    public ItemEstoqueModel toModel(ItemEstoque item, Integer quantidadeDisponivelVenda) {
        ItemEstoqueModel m = new ItemEstoqueModel();
        m.setId(item.getId());
        if (item.getMedicamento() != null) {
            m.setMedicamentoId(item.getMedicamento().getId());
            m.setMedicamentoNome(item.getMedicamento().getNomeComercial());
        }
        m.setQuantidadeAtual(item.getQuantidadeAtual());
        m.setQuantidadeMinima(item.getQuantidadeMinima());
        m.setQuantidadeMaxima(item.getQuantidadeMaxima());
        boolean semEntrada = item.getId() == null;
        m.setSemEntrada(semEntrada);
        m.setAbaixoDoMinimo(semEntrada ? false : item.estaAbaixoDoMinimo());
        m.setZerado(semEntrada ? false : item.estaZerado());
        if (quantidadeDisponivelVenda != null) {
            m.setQuantidadeDisponivelVenda(quantidadeDisponivelVenda);
        } else if (item.getMedicamento() != null) {
            m.setQuantidadeDisponivelVenda(0);
        }
        return m;
    }

    public List<ItemEstoqueModel> toItemCollection(List<ItemEstoque> itens) {
        return toItemCollection(itens, Collections.emptyMap());
    }

    public List<ItemEstoqueModel> toItemCollection(List<ItemEstoque> itens,
                                                   Map<UUID, Integer> disponivelVenda) {
        return itens.stream()
            .map(item -> {
                UUID medId = item.getMedicamento() != null ? item.getMedicamento().getId() : null;
                Integer disp = medId != null && disponivelVenda != null
                    ? disponivelVenda.getOrDefault(medId, 0)
                    : 0;
                return toModel(item, disp);
            })
            .toList();
    }

    public Page<ItemEstoqueModel> toItemPage(Page<ItemEstoque> page, Map<UUID, Integer> disponivelVenda) {
        return page.map(item -> {
            UUID medId = item.getMedicamento() != null ? item.getMedicamento().getId() : null;
            Integer disp = medId != null && disponivelVenda != null
                ? disponivelVenda.getOrDefault(medId, 0)
                : 0;
            return toModel(item, disp);
        });
    }

    public LoteModel toModel(Lote lote) {
        LoteModel m = new LoteModel();
        m.setId(lote.getId());
        if (lote.getMedicamento() != null) {
            m.setMedicamentoId(lote.getMedicamento().getId());
        }
        m.setNumeroLote(lote.getNumeroLote());
        m.setDataValidade(lote.getDataValidade());
        m.setQuantidadeAtual(lote.getQuantidadeAtual());
        m.setStatus(lote.getStatus() != null ? lote.getStatus().name() : null);
        if (lote.getDataValidade() != null) {
            m.setDiasParaVencer((int) java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), lote.getDataValidade()));
        }
        return m;
    }

    public List<LoteModel> toLoteCollection(List<Lote> lotes) {
        return lotes.stream().map(this::toModel).toList();
    }

    public AlertaEstoqueModel toModel(AlertaEstoque alerta) {
        AlertaEstoqueModel m = new AlertaEstoqueModel();
        m.setId(alerta.getId());
        if (alerta.getMedicamento() != null) {
            m.setMedicamentoId(alerta.getMedicamento().getId());
            m.setMedicamentoNome(alerta.getMedicamento().getNomeComercial());
        }
        if (alerta.getLote() != null) {
            m.setLoteId(alerta.getLote().getId());
            m.setNumeroLote(alerta.getLote().getNumeroLote());
        }
        m.setTipo(alerta.getTipo() != null ? alerta.getTipo().name() : null);
        m.setMensagem(alerta.getMensagem());
        m.setStatus(alerta.getStatus() != null ? alerta.getStatus().name() : null);
        return m;
    }

    public List<AlertaEstoqueModel> toAlertaCollection(List<AlertaEstoque> alertas) {
        return alertas.stream().map(this::toModel).toList();
    }

    public MovimentacaoEstoqueModel toModel(MovimentacaoEstoque mov) {
        MovimentacaoEstoqueModel m = new MovimentacaoEstoqueModel();
        m.setId(mov.getId());
        if (mov.getMedicamento() != null) {
            m.setMedicamentoId(mov.getMedicamento().getId());
            m.setMedicamentoNome(mov.getMedicamento().getNomeComercial());
        }
        if (mov.getLote() != null) {
            m.setLoteId(mov.getLote().getId());
            m.setNumeroLote(mov.getLote().getNumeroLote());
        }
        m.setTipo(mov.getTipo() != null ? mov.getTipo().name() : null);
        m.setQuantidade(mov.getQuantidade());
        m.setSaldoAnterior(mov.getSaldoAnterior());
        m.setSaldoPosterior(mov.getSaldoPosterior());
        m.setReferenciaId(mov.getReferenciaId());
        m.setMotivoAjuste(mov.getMotivoAjuste());
        m.setDataHora(mov.getDataHora());
        return m;
    }
}
