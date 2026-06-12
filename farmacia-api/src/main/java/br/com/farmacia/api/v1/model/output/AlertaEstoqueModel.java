package br.com.farmacia.api.v1.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AlertaEstoqueModel {
    private UUID id;
    private UUID medicamentoId;
    private String medicamentoNome;
    private UUID loteId;
    private String numeroLote;
    private String tipo;
    private String mensagem;
    private String status;
}
