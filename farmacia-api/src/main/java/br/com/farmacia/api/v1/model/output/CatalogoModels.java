package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Modelos resumidos para listagem/cadastro de entidades auxiliares do catálogo.
 */
public final class CatalogoModels {

    private CatalogoModels() {
    }

    @Getter
    @Setter
    public static class FabricanteModel {
        private UUID id;
        private String razaoSocial;
        private String nomeFantasia;
        private String cnpj;
        private Boolean ativo;
    }

    @Getter
    @Setter
    public static class CategoriaModel {
        private UUID id;
        private String nome;
        private String descricao;
        private Boolean ativo;
    }

    @Getter
    @Setter
    public static class PrescritorModel {
        private UUID id;
        private String nome;
        private String crm;
        private String ufCrm;
        private String especialidade;
        private String email;
        private Boolean ativo;
    }
}
