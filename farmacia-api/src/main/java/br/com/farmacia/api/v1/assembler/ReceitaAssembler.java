package br.com.farmacia.api.v1.assembler;

import br.com.farmacia.api.v1.model.ReceitaModel;
import br.com.farmacia.domain.receituario.entity.Receita;
import org.springframework.stereotype.Component;

@Component
public class ReceitaAssembler {

    public ReceitaModel toModel(Receita receita) {
        ReceitaModel m = new ReceitaModel();
        m.setId(receita.getId());
        m.setNumeroReceita(receita.getNumeroReceita());
        m.setDataEmissao(receita.getDataEmissao());
        m.setDataValidade(receita.getDataValidade());
        m.setTipo(receita.getTipo());
        m.setStatus(receita.getStatus());
        m.setCid(receita.getCid());
        m.setRetida(receita.getRetida());
        if (receita.getCliente() != null) {
            m.setClienteId(receita.getCliente().getId());
            m.setClienteNome(receita.getCliente().getNome());
        }
        if (receita.getPrescritor() != null) {
            m.setPrescritorId(receita.getPrescritor().getId());
            m.setPrescritorNome(receita.getPrescritor().getNome());
            m.setPrescritorCrm(receita.getPrescritor().getCrm() + "/" + receita.getPrescritor().getUfCrm());
        }
        m.setDataValidacao(receita.getDataValidacao());
        m.setMotivoRejeicao(receita.getMotivoRejeicao());
        return m;
    }
}
