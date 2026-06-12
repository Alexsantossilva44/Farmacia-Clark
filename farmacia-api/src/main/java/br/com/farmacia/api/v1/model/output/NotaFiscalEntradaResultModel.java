package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotaFiscalEntradaResultModel {
    private NotaFiscalEntradaModel nota;
    private ConferenciaNotaModel conferencia;
}
