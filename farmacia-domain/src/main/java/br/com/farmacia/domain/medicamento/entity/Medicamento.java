package br.com.farmacia.domain.medicamento.entity;

import br.com.farmacia.domain.medicamento.enums.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade raiz do agregado Medicamento.
 *
 * <p>Regras de domínio embutidas:</p>
 * <ul>
 *   <li>Medicamento controlado sempre requer receita</li>
 *   <li>PMC (Preço Máximo ao Consumidor) não pode ser negativo</li>
 * </ul>
 *
 * @author Alex Silva e Claude
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"principiosAtivos", "medicamentoControlado"})
public class Medicamento {

    private UUID   id;
    private String codigoEan;
    private String codigoAnvisa;
    private String nomeComercial;
    private String nomeGenerico;

    private TipoMedicamento    tipo;
    private FormaFarmaceutica  formaFarmaceutica;
    private String             concentracao;
    private String             apresentacao;
    private String             classeTerapeutica;

    private Boolean      requerReceita;
    private NivelControle nivelControle;

    private BigDecimal precoMaximoConsumidor;

    @Builder.Default
    private Boolean ativo = true;

    private Fabricante   fabricante;
    private Categoria    categoria;

    @Builder.Default
    private List<PrincipioAtivo> principiosAtivos = new ArrayList<>();

    private MedicamentoControlado medicamentoControlado;

    // ── Regras de Domínio ──────────────────────────────────────────────

    public boolean isControlado() {
        return nivelControle == NivelControle.CONTROLADO_B1
            || nivelControle == NivelControle.CONTROLADO_B2
            || nivelControle == NivelControle.CONTROLADO_C1
            || nivelControle == NivelControle.CONTROLADO_C2;
    }

    public boolean isAntimicrobiano() {

        return nivelControle == NivelControle.ANTIMICROBIANO;
    }

    public boolean requerCpfComprador() {

        return isControlado() || isAntimicrobiano();
    }

    public boolean isRequerReceita() {

        return Boolean.TRUE.equals(requerReceita) || isControlado();
    }

    /**
     * Atribui identidade ao agregado recém-criado (persistência).
     */
    public void atribuirId(UUID id) {
        if (this.id != null) {
            throw new IllegalStateException("Identidade já atribuída ao medicamento");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id não pode ser nulo");
        }
        this.id = id;
    }

    /**
     * Exclusão lógica — mantém histórico e integridade referencial.
     */
    public void inativar() {
        this.ativo = false;
    }

    /**
     * Vincula dados de controle especial (SNGPC / portaria).
     */
    public void associarMedicamentoControlado(MedicamentoControlado controlado) {
        this.medicamentoControlado = controlado;
    }

    /**
     * Atualiza dados cadastrais preservando identidade e estado de ativação.
     */
    public void atualizar(
            String codigoEan,
            String codigoAnvisa,
            String nomeComercial,
            String nomeGenerico,
            TipoMedicamento tipo,
            FormaFarmaceutica formaFarmaceutica,
            String concentracao,
            String apresentacao,
            String classeTerapeutica,
            Boolean requerReceita,
            NivelControle nivelControle,
            BigDecimal precoMaximoConsumidor,
            Fabricante fabricante,
            Categoria categoria,
            List<PrincipioAtivo> principiosAtivos) {

        if (precoMaximoConsumidor != null && precoMaximoConsumidor.signum() < 0) {
            throw new IllegalArgumentException("PMC não pode ser negativo");
        }

        this.codigoEan = codigoEan;
        this.codigoAnvisa = codigoAnvisa;
        this.nomeComercial = nomeComercial;
        this.nomeGenerico = nomeGenerico;
        this.tipo = tipo;
        this.formaFarmaceutica = formaFarmaceutica;
        this.concentracao = concentracao;
        this.apresentacao = apresentacao;
        this.classeTerapeutica = classeTerapeutica;
        this.requerReceita = requerReceita;
        this.nivelControle = nivelControle;
        this.precoMaximoConsumidor = precoMaximoConsumidor;
        this.fabricante = fabricante;
        this.categoria = categoria;

        if (principiosAtivos != null) {
            this.principiosAtivos.clear();
            this.principiosAtivos.addAll(principiosAtivos);
        }
    }

    /**
     * Define o Preço Máximo ao Consumidor (PMC).
     */
    public void definirPrecoMaximoConsumidor(BigDecimal precoMaximoConsumidor) {
        if (precoMaximoConsumidor != null && precoMaximoConsumidor.signum() < 0) {
            throw new IllegalArgumentException("PMC não pode ser negativo");
        }
        this.precoMaximoConsumidor = precoMaximoConsumidor;
    }
}
