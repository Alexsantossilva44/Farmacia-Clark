package br.com.farmacia.api.v1.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class LoteModel {
    private UUID id;
    private UUID medicamentoId;
    private String numeroLote;
    private LocalDate dataValidade;
    private Integer quantidadeAtual;
    private String status;
    private Integer diasParaVencer;
}
