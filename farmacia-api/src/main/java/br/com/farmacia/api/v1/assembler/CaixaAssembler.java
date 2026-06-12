package br.com.farmacia.api.v1.assembler;

import br.com.farmacia.api.v1.model.CaixaModel;
import br.com.farmacia.domain.financeiro.entity.Caixa;
import org.springframework.stereotype.Component;

@Component
public class CaixaAssembler {

    public CaixaModel toModel(Caixa caixa) {
        CaixaModel m = new CaixaModel();
        m.setId(caixa.getId());
        if (caixa.getPdv() != null) {
            m.setPdvId(caixa.getPdv().getId());
            m.setPdvNumero(caixa.getPdv().getNumero());
        }
        if (caixa.getFuncionario() != null) {
            m.setFuncionarioId(caixa.getFuncionario().getId());
            m.setFuncionarioNome(caixa.getFuncionario().getNome());
        }
        m.setAbertura(caixa.getAbertura());
        m.setFechamento(caixa.getFechamento());
        m.setSaldoAbertura(caixa.getSaldoAbertura());
        m.setSaldoFechamento(caixa.getSaldoFechamento());
        m.setTotalVendas(caixa.getTotalVendas());
        m.setStatus(caixa.getStatus() != null ? caixa.getStatus().name() : null);
        return m;
    }
}
