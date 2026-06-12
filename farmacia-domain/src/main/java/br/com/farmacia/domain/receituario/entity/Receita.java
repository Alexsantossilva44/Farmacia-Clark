package br.com.farmacia.domain.receituario.entity;

import br.com.farmacia.domain.cliente.entity.Cliente;
import br.com.farmacia.domain.funcionario.entity.Farmaceutico;
import br.com.farmacia.domain.receituario.enums.StatusReceita;
import br.com.farmacia.domain.receituario.enums.TipoReceita;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade raiz do agregado Receituário.
 *
 * <p>Regras de domínio:</p>
 * <ul>
 *   <li>Receitas Azul, Amarela e Branca Especial devem ser retidas</li>
 *   <li>Validade varia por tipo (30 dias para maioria, 10 dias para antimicrobiano)</li>
 * </ul>
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Receita {

    private UUID          id;
    private String        numeroReceita;
    private LocalDate     dataEmissao;
    private LocalDate     dataValidade;
    private TipoReceita   tipo;

    @Builder.Default
    private StatusReceita status = StatusReceita.PENDENTE;

    private String        cid;

    @Builder.Default
    private Boolean retida = false;

    private String        imagemPath;
    private String        motivoRejeicao;
    private Prescritor    prescritor;
    private Cliente       cliente;
    private UUID          farmaceuticoId;
    private Farmaceutico  farmaceutico;
    private LocalDateTime dataValidacao;

    // ── Regras de Domínio ──────────────────────────────────────────────

    public boolean estaValida() {
        return !LocalDate.now().isAfter(this.dataValidade) // C-09: inclui o próprio dia de validade
            && this.status == StatusReceita.APROVADA;
    }

    public boolean requerRetencao() {
        return tipo == TipoReceita.AZUL
            || tipo == TipoReceita.AMARELA
            || tipo == TipoReceita.BRANCA_ESPECIAL;
    }

    public boolean estaVencida() {
        // C-09 corrigido: dataValidade é o último dia válido (inclusive); isAfter() é estritamente
        // "depois de", garantindo mutex com estaValida() que usa !isAfter() (inclusive o dia).
        return LocalDate.now().isAfter(this.dataValidade);
    }

    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída à receita");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    /**
     * Marca receita como utilizada após dispensação em venda.
     */
    public void marcarComoUtilizada() {
        if (status != StatusReceita.APROVADA) {
            throw new IllegalStateException(
                "Receita deve estar aprovada para ser utilizada. Status: " + status);
        }
        this.status = StatusReceita.UTILIZADA;
    }

    /**
     * Aprova receita após validação pelo farmacêutico.
     */
    public void aprovar(Farmaceutico farmaceuticoValidador) {
        if (farmaceuticoValidador == null) {
            throw new IllegalArgumentException("Farmacêutico validador é obrigatório");
        }
        this.status = StatusReceita.APROVADA;
        this.farmaceutico = farmaceuticoValidador;
        this.farmaceuticoId = farmaceuticoValidador.getId();
        this.dataValidacao = LocalDateTime.now();
        if (requerRetencao()) {
            this.retida = true;
        }
    }

    /**
     * Rejeita receita com motivo consolidado das violações encontradas.
     */
    public void rejeitar(Farmaceutico farmaceuticoValidador, String motivo) {
        if (farmaceuticoValidador == null) {
            throw new IllegalArgumentException("Farmacêutico validador é obrigatório");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Motivo da rejeição é obrigatório");
        }
        this.status = StatusReceita.REJEITADA;
        this.farmaceutico = farmaceuticoValidador;
        this.farmaceuticoId = farmaceuticoValidador.getId();
        this.dataValidacao = LocalDateTime.now();
        this.motivoRejeicao = motivo;
    }
}
