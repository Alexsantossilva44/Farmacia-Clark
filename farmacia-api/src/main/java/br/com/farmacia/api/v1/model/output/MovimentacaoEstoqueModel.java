package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MovimentacaoEstoqueModel {
    private UUID id;
    private UUID medicamentoId;
    private String medicamentoNome;
    private UUID loteId;
    private String numeroLote;
    private String tipo;
    private Integer quantidade;
    private Integer saldoAnterior;
    private Integer saldoPosterior;
    private UUID referenciaId;
    private String motivoAjuste;
    private LocalDateTime dataHora;
}
