package br.com.farmacia.qa.bdd.steps;

import br.com.farmacia.domain.estoque.entity.Lote;
import br.com.farmacia.domain.estoque.enums.StatusLote;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.qa.bdd.BddTestContext;
import br.com.farmacia.qa.builder.FarmaciaTestBuilders.MedicamentoBuilder;
import br.com.farmacia.qa.seed.IntegracaoTestSeed;
import io.cucumber.java.pt.Dado;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

/**
 * Steps compartilhados de estoque/lote usados por features de venda e alertas.
 */
public class LoteComumSteps {

    @Autowired private IntegracaoTestSeed testSeed;
    @Autowired private BddTestContext bddContext;

    @Dado("que o lote {string} de {string} vence em {int} dias")
    public void queLoteDeMedicamentoVenceEmDias(String numeroLote, String nomeMed, int dias) {
        bddContext.setNumeroLoteAtual(numeroLote);

        Medicamento medicamento = testSeed.garantirMedicamento(nomeMed, b ->
            "Rivotril 2mg".equals(nomeMed) ? MedicamentoBuilder.umMedicamentoControlado() : b);
        bddContext.setMedicamentoAtual(medicamento);

        Lote lote = testSeed.garantirLote(
            numeroLote,
            medicamento,
            LocalDate.now().plusDays(dias),
            30,
            StatusLote.ATIVO
        );
        bddContext.setLoteAtual(lote);
    }
}
