package br.com.farmacia.api.v1.model;

import br.com.farmacia.domain.cliente.valueobject.EnderecoVO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ClienteModel {
    private UUID id;
    private String nome;
    private String cpf;
    private LocalDate dataNascimento;
    private String sexo;
    private String telefone;
    private String email;
    private EnderecoVO endereco;
    private String alergias;
    private String observacoes;
    private Boolean ativo;
    private LocalDateTime dataCadastro;
}
