package br.com.farmacia.domain.medicamento.repository;

import br.com.farmacia.domain.medicamento.entity.Medicamento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída (Port) do domínio para persistência de medicamentos.
 * A implementação fica na infra (Adapter).
 *
 * @author Alex Silva e Claude
 */
public interface MedicamentoRepository {
    Medicamento save(Medicamento medicamento);
    Optional<Medicamento> findById(UUID id);
    Optional<Medicamento> findByNomeComercial(String nome);
    Optional<Medicamento> findByCodigoEan(String ean);
    Optional<Medicamento> findByCodigoAnvisa(String codigo);
    List<Medicamento> findAll();

    List<Medicamento> findAllOrdenados(int offset, int limit, String busca);

    long contar(String busca);

    List<Medicamento> findAtivosOrdenados(int offset, int limit, String nomeFiltro);

    long contarAtivos(String nomeFiltro);

    void deleteById(UUID id);
    boolean existsByCodigoEan(String ean);
    boolean existsByNomeComercial(String nomeComercial);
    boolean existsByNomeComercialExcluindo(String nomeComercial, UUID idExcluir);
    Medicamento getReferenceById(UUID id);
}
