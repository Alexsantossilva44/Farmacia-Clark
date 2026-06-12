package br.com.farmacia.api.v1.assembler;

import br.com.farmacia.api.v1.model.MedicamentoModel;
import br.com.farmacia.api.v1.model.input.MedicamentoInput;
import br.com.farmacia.domain.medicamento.entity.Categoria;
import br.com.farmacia.domain.medicamento.entity.Fabricante;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.entity.PrincipioAtivo;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Assembler de Medicamento.
 *
 * <p><b>Heurística AlgaWorks</b>: a classe {@code XxxAssembler} centraliza
 * toda a lógica de conversão entre entidade de domínio e DTOs da API.
 * O mapeamento é explícito (sem reflexão), alinhado aos demais assemblers
 * e aos mappers de persistência da infraestrutura.</p>
 *
 * <p>Separar a lógica de conversão do Controller evita "fat controllers"
 * e facilita testes unitários do mapeamento de forma isolada.</p>
 */
@Component
public class MedicamentoAssembler {

    // ─── Entidade → Model ─────────────────────────────────────────────────────

    public MedicamentoModel toModel(Medicamento medicamento) {
        if (medicamento == null) {
            return null;
        }
        MedicamentoModel model = new MedicamentoModel();
        model.setId(medicamento.getId());
        model.setCodigoEan(medicamento.getCodigoEan());
        model.setCodigoAnvisa(medicamento.getCodigoAnvisa());
        model.setNomeComercial(medicamento.getNomeComercial());
        model.setNomeGenerico(medicamento.getNomeGenerico());
        model.setTipo(medicamento.getTipo());
        model.setFormaFarmaceutica(medicamento.getFormaFarmaceutica());
        model.setConcentracao(medicamento.getConcentracao());
        model.setApresentacao(medicamento.getApresentacao());
        model.setClasseTerapeutica(medicamento.getClasseTerapeutica());
        model.setRequerReceita(medicamento.getRequerReceita());
        model.setNivelControle(medicamento.getNivelControle());
        model.setPrecoMaximoConsumidor(medicamento.getPrecoMaximoConsumidor());
        model.setAtivo(medicamento.getAtivo());
        model.setFabricante(toFabricanteModel(medicamento.getFabricante()));
        model.setCategoria(toCategoriaModel(medicamento.getCategoria()));
        if (medicamento.getPrincipiosAtivos() != null && !medicamento.getPrincipiosAtivos().isEmpty()) {
            model.setPrincipiosAtivos(medicamento.getPrincipiosAtivos().stream()
                .map(this::toPrincipioAtivoModel)
                .toList());
        }
        return model;
    }

    public List<MedicamentoModel> toCollectionModel(List<Medicamento> medicamentos) {
        return medicamentos.stream()
            .map(this::toModel)
            .toList();
    }

    // ─── Input → Entidade ─────────────────────────────────────────────────────

    /**
     * Converte o DTO de entrada em uma nova entidade de domínio.
     * Não persiste — apenas monta o objeto.
     */
    public Medicamento toEntity(MedicamentoInput input) {
        return Medicamento.builder()
            .codigoEan(input.getCodigoEan())
            .codigoAnvisa(input.getCodigoAnvisa())
            .nomeComercial(input.getNomeComercial())
            .nomeGenerico(input.getNomeGenerico())
            .tipo(input.getTipo())
            .formaFarmaceutica(input.getFormaFarmaceutica())
            .concentracao(input.getConcentracao())
            .apresentacao(input.getApresentacao())
            .classeTerapeutica(input.getClasseTerapeutica())
            .requerReceita(input.getRequerReceita())
            .nivelControle(input.getNivelControle())
            .precoMaximoConsumidor(input.getPrecoMaximoConsumidor())
            .fabricante(toFabricante(input.getFabricante()))
            .categoria(toCategoria(input.getCategoria()))
            .principiosAtivos(toPrincipiosAtivos(input.getPrincipiosAtivos()))
            .build();
    }

    /**
     * Aplica as alterações do DTO em uma entidade já existente (para PUT).
     */
    public void copyToEntity(MedicamentoInput input, Medicamento medicamentoDestino) {
        medicamentoDestino.atualizar(
            input.getCodigoEan(),
            input.getCodigoAnvisa(),
            input.getNomeComercial(),
            input.getNomeGenerico(),
            input.getTipo(),
            input.getFormaFarmaceutica(),
            input.getConcentracao(),
            input.getApresentacao(),
            input.getClasseTerapeutica(),
            input.getRequerReceita(),
            input.getNivelControle(),
            input.getPrecoMaximoConsumidor(),
            toFabricante(input.getFabricante()),
            toCategoria(input.getCategoria()),
            input.getPrincipiosAtivos() != null
                ? toPrincipiosAtivos(input.getPrincipiosAtivos())
                : null
        );
    }

    private MedicamentoModel.FabricanteResumoModel toFabricanteModel(Fabricante fabricante) {
        if (fabricante == null) {
            return null;
        }
        MedicamentoModel.FabricanteResumoModel model = new MedicamentoModel.FabricanteResumoModel();
        model.setId(fabricante.getId());
        model.setRazaoSocial(fabricante.getRazaoSocial());
        model.setNomeFantasia(fabricante.getNomeFantasia());
        return model;
    }

    private MedicamentoModel.CategoriaResumoModel toCategoriaModel(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        MedicamentoModel.CategoriaResumoModel model = new MedicamentoModel.CategoriaResumoModel();
        model.setId(categoria.getId());
        model.setNome(categoria.getNome());
        return model;
    }

    private MedicamentoModel.PrincipioAtivoResumoModel toPrincipioAtivoModel(PrincipioAtivo principio) {
        if (principio == null) {
            return null;
        }
        MedicamentoModel.PrincipioAtivoResumoModel model = new MedicamentoModel.PrincipioAtivoResumoModel();
        model.setId(principio.getId());
        model.setNome(principio.getNome());
        model.setDcb(principio.getDcb());
        return model;
    }

    private Fabricante toFabricante(MedicamentoInput.FabricanteIdInput fabricante) {
        if (fabricante == null || fabricante.getId() == null) {
            return null;
        }
        return Fabricante.builder().id(fabricante.getId()).build();
    }

    private Categoria toCategoria(MedicamentoInput.CategoriaIdInput categoria) {
        if (categoria == null || categoria.getId() == null) {
            return null;
        }
        return Categoria.builder().id(categoria.getId()).build();
    }

    private List<PrincipioAtivo> toPrincipiosAtivos(List<MedicamentoInput.PrincipioAtivoIdInput> principios) {
        if (principios == null) {
            return Collections.emptyList();
        }
        return principios.stream()
            .map(p -> PrincipioAtivo.builder().id(p.getId()).build())
            .toList();
    }
}
