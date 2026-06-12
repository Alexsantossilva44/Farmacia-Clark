package br.com.farmacia.api.v1.model.output;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class DivergenciaConferenciaModel {
    private String tipo;
    private UUID medicamentoId;
    private String medicamentoNome;
    private Integer quantidadeEsperada;
    private Integer quantidadeRecebida;
    private java.math.BigDecimal precoEsperado;
    private java.math.BigDecimal precoRecebido;
    private String mensagem;
}
