package br.com.farmacia.api.v1.model.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DisponivelVendaModel {

    private UUID medicamentoId;
    private int quantidadeDisponivelVenda;
}
