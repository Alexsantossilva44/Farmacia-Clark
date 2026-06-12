package br.com.farmacia.domain.receituario.enums;

/**
 * Tipo de receita conforme Portaria ANVISA 344/98.
 * @author Alex Silva e Claude
 */
public enum TipoReceita {
    SIMPLES,
    AZUL,             // C1 — 2 vias — válida 30 dias
    AMARELA,          // C2 — 2 vias — válida 30 dias
    BRANCA_ESPECIAL,  // B1 — 3 vias — válida 30 dias
    ANTIMICROBIANO;   // 2 vias — válida 10 dias (RDC 20/2011)

    public int getValidadeDias() {
        return this == ANTIMICROBIANO ? 10 : 30;
    }
}
