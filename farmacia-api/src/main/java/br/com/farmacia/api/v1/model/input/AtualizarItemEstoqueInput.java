package br.com.farmacia.api.v1.model.input;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AtualizarItemEstoqueInput {
    private Integer quantidadeMinima;
    private Integer quantidadeMaxima;
}
