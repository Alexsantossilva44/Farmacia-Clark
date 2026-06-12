package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ConferenciaNotaModel {
    private boolean conferida;
    private String statusNota;
    private UUID pedidoCompraId;
    private String statusPedido;
    private List<DivergenciaConferenciaModel> divergencias;
}
