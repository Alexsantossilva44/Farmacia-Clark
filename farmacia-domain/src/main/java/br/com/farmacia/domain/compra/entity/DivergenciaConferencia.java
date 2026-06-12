package br.com.farmacia.domain.compra.entity;

import br.com.farmacia.domain.compra.enums.TipoDivergenciaConferencia;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DivergenciaConferencia {

    private TipoDivergenciaConferencia tipo;
    private UUID medicamentoId;
    private String medicamentoNome;
    private Integer quantidadeEsperada;
    private Integer quantidadeRecebida;
    private BigDecimal precoEsperado;
    private BigDecimal precoRecebido;
    private String mensagem;
}
