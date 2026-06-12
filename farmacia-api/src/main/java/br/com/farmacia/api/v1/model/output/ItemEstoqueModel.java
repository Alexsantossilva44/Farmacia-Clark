package br.com.farmacia.api.v1.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ItemEstoqueModel {
    private UUID id;
    private UUID medicamentoId;
    private String medicamentoNome;
    private Integer quantidadeAtual;
    private Integer quantidadeMinima;
    private Integer quantidadeMaxima;
    private Boolean abaixoDoMinimo;
    private Boolean zerado;
    /** true quando o medicamento ainda não teve nenhuma entrada registrada */
    private Boolean semEntrada;
    /** Saldo em lotes ATIVO e não vencidos (o que o PDV pode dispensar). */
    private Integer quantidadeDisponivelVenda;
}
