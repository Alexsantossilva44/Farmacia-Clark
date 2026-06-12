package br.com.farmacia.infrastructure.persistence.cliente;

import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.cliente.valueobject.EnderecoVO;

import java.time.LocalDateTime;

/**
 * Mapeamento manual entre {@link Cliente} (domínio) e {@link ClienteJpaEntity}.
 *
 * @author Alex Silva e Claude
 */
public final class ClientePersistenceMapper {

    private ClientePersistenceMapper() {
    }

    public static ClienteJpaEntity toJpa(Cliente c) {
        EnderecoVO endereco = c.getEndereco();
        return ClienteJpaEntity.builder()
            .id(c.getId())
            .nome(c.getNome())
            .cpf(c.getCpf())
            .dataNascimento(c.getDataNascimento())
            .sexo(c.getSexo())
            .telefone(c.getTelefone())
            .email(c.getEmail())
            .logradouro(endereco != null ? endereco.getLogradouro() : null)
            .numero(endereco != null ? endereco.getNumero() : null)
            .complemento(endereco != null ? endereco.getComplemento() : null)
            .bairro(endereco != null ? endereco.getBairro() : null)
            .cidade(endereco != null ? endereco.getCidade() : null)
            .uf(endereco != null ? endereco.getUf() : null)
            .cep(endereco != null ? endereco.getCep() : null)
            .alergias(c.getAlergias())
            .observacoes(c.getObservacoes())
            .dataCadastro(c.getDataCadastro() != null ? c.getDataCadastro() : LocalDateTime.now())
            .ativo(c.getAtivo() != null ? c.getAtivo() : Boolean.TRUE)
            .build();
    }

    public static Cliente toDomain(ClienteJpaEntity e) {
        return Cliente.builder()
            .id(e.getId())
            .nome(e.getNome())
            .cpf(e.getCpf())
            .dataNascimento(e.getDataNascimento())
            .sexo(e.getSexo())
            .telefone(e.getTelefone())
            .email(e.getEmail())
            .endereco(temEndereco(e)
                ? EnderecoVO.builder()
                    .logradouro(e.getLogradouro())
                    .numero(e.getNumero())
                    .complemento(e.getComplemento())
                    .bairro(e.getBairro())
                    .cidade(e.getCidade())
                    .uf(e.getUf())
                    .cep(e.getCep())
                    .build()
                : null)
            .alergias(e.getAlergias())
            .observacoes(e.getObservacoes())
            .dataCadastro(e.getDataCadastro())
            .ativo(e.getAtivo())
            .build();
    }

    private static boolean temEndereco(ClienteJpaEntity e) {
        return e.getLogradouro() != null || e.getCidade() != null || e.getCep() != null;
    }
}
