package br.com.farmacia.domain.compra.entity;

import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fornecedor {

    private UUID id;
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private Boolean ativo;

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao fornecedor");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }
}
