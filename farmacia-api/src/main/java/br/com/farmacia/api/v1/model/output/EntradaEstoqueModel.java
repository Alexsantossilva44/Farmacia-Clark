package br.com.farmacia.api.v1.model.output;

import br.com.farmacia.api.v1.model.ItemEstoqueModel;
import br.com.farmacia.api.v1.model.LoteModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntradaEstoqueModel {
    private ItemEstoqueModel itemEstoque;
    private LoteModel lote;
}
