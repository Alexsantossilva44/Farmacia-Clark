package br.com.farmacia.domain.estoque.entity;

import br.com.farmacia.domain.medicamento.entity.Medicamento;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade: Saldo consolidado de estoque por medicamento.
 * É o ponto central de controle de estoque.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ItemEstoque {

    private UUID          id;
    private Medicamento   medicamento;
    private Integer       quantidadeAtual;
    private Integer       quantidadeMinima;
    private Integer       quantidadeMaxima;
    private LocalDateTime ultimaMovimentacao;

    public boolean estaAbaixoDoMinimo() {
        return quantidadeAtual != null
            && quantidadeMinima != null
            && quantidadeAtual < quantidadeMinima;
    }

    public boolean estaZerado() {
        return quantidadeAtual == null || quantidadeAtual == 0;
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao item de estoque");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    public void incrementarSaldo(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva");
        }
        quantidadeAtual = (quantidadeAtual != null ? quantidadeAtual : 0) + quantidade;
        ultimaMovimentacao = LocalDateTime.now();
    }

    public void atualizarLimites(Integer quantidadeMinima, Integer quantidadeMaxima) {
        if (quantidadeMinima != null) {
            this.quantidadeMinima = quantidadeMinima;
        }
        if (quantidadeMaxima != null) {
            this.quantidadeMaxima = quantidadeMaxima;
        }
    }
}
