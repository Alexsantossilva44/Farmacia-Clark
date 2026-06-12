package br.com.farmacia.domain.medicamento.enums;

/**
 * Nível de controle regulatório do medicamento (Portaria ANVISA 344/98).
 *
 * <ul>
 *   <li>LIVRE         — venda sem receita</li>
 *   <li>RECEITA_SIMPLES — receita comum</li>
 *   <li>CONTROLADO_C1  — psicotrópico, receita Azul</li>
 *   <li>CONTROLADO_C2  — retinoides, receita Amarela</li>
 *   <li>CONTROLADO_B1  — entorpecente, receita Branca Especial</li>
 *   <li>CONTROLADO_B2  — entorpecente veterinário</li>
 *   <li>ANTIMICROBIANO — receita + retenção (RDC 20/2011)</li>
 * </ul>
 *
 * @author Alex Silva e Claude
 */
public enum NivelControle {
    LIVRE,
    RECEITA_SIMPLES,
    CONTROLADO_C1,
    CONTROLADO_C2,
    CONTROLADO_B1,
    CONTROLADO_B2,
    ANTIMICROBIANO;

    public String getTipoReceitaRequerido() {
        return switch (this) {
            case CONTROLADO_B1 -> "BRANCA_ESPECIAL";
            case CONTROLADO_C1 -> "AZUL";
            case CONTROLADO_C2 -> "AMARELA";
            case ANTIMICROBIANO -> "SIMPLES ou ANTIMICROBIANO";
            default -> "SIMPLES";
        };
    }
}
