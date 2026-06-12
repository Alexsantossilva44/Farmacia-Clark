package br.com.farmacia.api.v1.assembler;

import br.com.farmacia.api.v1.model.ClienteModel;
import br.com.farmacia.domain.cliente.entity.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteAssembler {

    public ClienteModel toModel(Cliente cliente) {
        ClienteModel m = new ClienteModel();
        m.setId(cliente.getId());
        m.setNome(cliente.getNome());
        m.setCpf(cliente.getCpf());
        m.setDataNascimento(cliente.getDataNascimento());
        m.setSexo(cliente.getSexo());
        m.setTelefone(cliente.getTelefone());
        m.setEmail(cliente.getEmail());
        m.setEndereco(cliente.getEndereco());
        m.setAlergias(cliente.getAlergias());
        m.setObservacoes(cliente.getObservacoes());
        m.setAtivo(cliente.getAtivo());
        m.setDataCadastro(cliente.getDataCadastro());
        return m;
    }
}
