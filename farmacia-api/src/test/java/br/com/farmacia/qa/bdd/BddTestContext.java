package br.com.farmacia.qa.bdd;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import org.springframework.stereotype.Component;

/**
 * Estado compartilhado entre classes de steps Cucumber.
 *
 * <p>Evita duplicar step definitions idênticas e permite que steps de
 * domínios diferentes (venda, alertas) reutilizem o mesmo lote semeado.</p>
 */
@Component
public class BddTestContext {

    private Medicamento medicamentoAtual;
    private Lote loteAtual;
    private String numeroLoteAtual;

    public Medicamento getMedicamentoAtual() {
        return medicamentoAtual;
    }

    public void setMedicamentoAtual(Medicamento medicamentoAtual) {
        this.medicamentoAtual = medicamentoAtual;
    }

    public Lote getLoteAtual() {
        return loteAtual;
    }

    public void setLoteAtual(Lote loteAtual) {
        this.loteAtual = loteAtual;
    }

    public String getNumeroLoteAtual() {
        return numeroLoteAtual;
    }

    public void setNumeroLoteAtual(String numeroLoteAtual) {
        this.numeroLoteAtual = numeroLoteAtual;
    }

    public void limpar() {
        medicamentoAtual = null;
        loteAtual = null;
        numeroLoteAtual = null;
    }
}
