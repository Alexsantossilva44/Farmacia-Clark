package br.com.farmacia.api.v1.assembler;

import br.com.farmacia.api.v1.model.VendaModel;
import br.com.farmacia.api.v1.model.input.VendaInput;
import br.com.farmacia.application.venda.usecase.RealizarVendaUseCase;
import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.financeiro.entity.PDV;
import br.com.farmacia.domain.funcionario.entity.Funcionario;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.receituario.entity.Receita;
import br.com.farmacia.domain.venda.entity.ItemVenda;
import br.com.farmacia.domain.venda.entity.Pagamento;
import br.com.farmacia.domain.venda.entity.Venda;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembler de Venda.
 *
 * <p><b>Heurística AlgaWorks</b>: quando o DTO de entrada não mapeia
 * diretamente para uma entidade (caso de Use Cases com {@code record Input}),
 * o Assembler monta o objeto de input do Use Case a partir do DTO.
 * Conversões entidade ↔ model são explícitas, sem biblioteca de reflexão.</p>
 */
@Component
public class VendaAssembler {

    // ─── VendaInput (DTO) → RealizarVendaUseCase.Input ────────────────────

    public RealizarVendaUseCase.Input toUseCaseInput(VendaInput dto) {
        return new RealizarVendaUseCase.Input(
            dto.getPdv().getId(),
            dto.getFuncionario().getId(),
            dto.getCliente() != null ? dto.getCliente().getId() : null,
            dto.getReceita() != null ? dto.getReceita().getId() : null,
            mapItens(dto.getItens()),
            mapPagamentos(dto.getPagamentos()),
            dto.getCompradorCpf(),
            dto.getCompradorNome()
        );
    }

    private List<RealizarVendaUseCase.Input.ItemInput> mapItens(
            List<VendaInput.ItemVendaInput> itens) {

        return itens.stream()
            .map(i -> new RealizarVendaUseCase.Input.ItemInput(
                i.getMedicamento().getId(),
                i.getQuantidade(),
                i.getPrecoUnitario(),
                i.getDesconto()
            ))
            .toList();
    }

    private List<RealizarVendaUseCase.Input.PagamentoInput> mapPagamentos(
            List<VendaInput.PagamentoInput> pagamentos) {

        return pagamentos.stream()
            .map(p -> new RealizarVendaUseCase.Input.PagamentoInput(
                p.getForma(),
                p.getValor()
            ))
            .toList();
    }

    // ─── Venda (entidade) → VendaModel (DTO de saída) ─────────────────────

    public VendaModel toModel(Venda venda) {
        if (venda == null) {
            return null;
        }
        VendaModel model = new VendaModel();
        model.setId(venda.getId());
        model.setNumeroCupom(venda.getNumeroCupom());
        model.setDataHora(venda.getDataHora());
        model.setStatus(venda.getStatus());
        model.setTipoAtendimento(venda.getTipoAtendimento());
        model.setSubtotal(venda.getSubtotal());
        model.setDesconto(venda.getDesconto());
        model.setTotal(venda.getTotal());
        model.setObservacao(venda.getObservacao());
        model.setCliente(toClienteResumo(venda.getCliente()));
        model.setFuncionario(toFuncionarioResumo(venda.getFuncionario()));
        model.setPdv(toPdvResumo(venda.getPdv()));
        model.setReceita(toReceitaResumo(venda.getReceita()));
        if (venda.getItens() != null && !venda.getItens().isEmpty()) {
            model.setItens(venda.getItens().stream().map(this::toItemModel).toList());
        }
        if (venda.getPagamentos() != null && !venda.getPagamentos().isEmpty()) {
            model.setPagamentos(venda.getPagamentos().stream().map(this::toPagamentoModel).toList());
        }
        return model;
    }

    public List<VendaModel> toCollectionModel(List<Venda> vendas) {
        return vendas.stream().map(this::toModel).toList();
    }

    private VendaModel.ClienteResumoModel toClienteResumo(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        VendaModel.ClienteResumoModel model = new VendaModel.ClienteResumoModel();
        model.setId(cliente.getId());
        model.setNome(cliente.getNome());
        model.setCpf(cliente.getCpf());
        return model;
    }

    private VendaModel.FuncionarioResumoModel toFuncionarioResumo(Funcionario funcionario) {
        if (funcionario == null) {
            return null;
        }
        VendaModel.FuncionarioResumoModel model = new VendaModel.FuncionarioResumoModel();
        model.setId(funcionario.getId());
        model.setNome(funcionario.getNome());
        return model;
    }

    private VendaModel.PDVResumoModel toPdvResumo(PDV pdv) {
        if (pdv == null) {
            return null;
        }
        VendaModel.PDVResumoModel model = new VendaModel.PDVResumoModel();
        model.setId(pdv.getId());
        model.setNumero(pdv.getNumero());
        return model;
    }

    private VendaModel.ReceitaResumoModel toReceitaResumo(Receita receita) {
        if (receita == null) {
            return null;
        }
        VendaModel.ReceitaResumoModel model = new VendaModel.ReceitaResumoModel();
        model.setId(receita.getId());
        model.setNumeroReceita(receita.getNumeroReceita());
        model.setTipo(receita.getTipo() != null ? receita.getTipo().name() : null);
        model.setRetida(receita.getRetida());
        return model;
    }

    private VendaModel.ItemVendaModel toItemModel(ItemVenda item) {
        VendaModel.ItemVendaModel model = new VendaModel.ItemVendaModel();
        model.setId(item.getId());
        model.setQuantidade(item.getQuantidade());
        model.setPrecoUnitario(item.getPrecoUnitario());
        model.setDesconto(item.getDesconto());
        model.setSubtotal(item.getSubtotal());
        model.setMedicamento(toMedicamentoResumo(item.getMedicamento()));
        model.setLote(toLoteResumo(item.getLote()));
        return model;
    }

    private VendaModel.ItemVendaModel.MedicamentoResumoModel toMedicamentoResumo(Medicamento medicamento) {
        if (medicamento == null) {
            return null;
        }
        VendaModel.ItemVendaModel.MedicamentoResumoModel model =
            new VendaModel.ItemVendaModel.MedicamentoResumoModel();
        model.setId(medicamento.getId());
        model.setNomeComercial(medicamento.getNomeComercial());
        model.setNomeGenerico(medicamento.getNomeGenerico());
        model.setConcentracao(medicamento.getConcentracao());
        return model;
    }

    private VendaModel.ItemVendaModel.LoteResumoModel toLoteResumo(Lote lote) {
        if (lote == null) {
            return null;
        }
        VendaModel.ItemVendaModel.LoteResumoModel model = new VendaModel.ItemVendaModel.LoteResumoModel();
        model.setId(lote.getId());
        model.setNumeroLote(lote.getNumeroLote());
        model.setDataValidade(lote.getDataValidade());
        return model;
    }

    private VendaModel.PagamentoModel toPagamentoModel(Pagamento pagamento) {
        VendaModel.PagamentoModel model = new VendaModel.PagamentoModel();
        model.setId(pagamento.getId());
        model.setForma(pagamento.getForma());
        model.setValor(pagamento.getValor());
        model.setTroco(pagamento.getTroco());
        model.setStatus(pagamento.getStatus());
        model.setDataHora(pagamento.getDataHora());
        return model;
    }
}
