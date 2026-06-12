package br.com.farmacia.infrastructure.persistence.medicamento;

import br.com.farmacia.domain.medicamento.entity.Categoria;
import br.com.farmacia.domain.medicamento.entity.Fabricante;
import br.com.farmacia.domain.medicamento.entity.Medicamento;
import br.com.farmacia.domain.medicamento.entity.MedicamentoControlado;
import br.com.farmacia.domain.medicamento.repository.MedicamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída que implementa a porta de domínio {@link MedicamentoRepository}
 * sobre o Spring Data JPA ({@link MedicamentoJpaRepository}).
 *
 * @author Alex Silva e Claude
 */
@Repository
@RequiredArgsConstructor
public class MedicamentoRepositoryAdapter implements MedicamentoRepository {

    private final MedicamentoJpaRepository jpaRepository;
    private final FabricanteJpaRepository fabricanteJpaRepository;
    private final CategoriaJpaRepository categoriaJpaRepository;
    private final MedicamentoControladoJpaRepository controladoJpaRepository;

    @Override
    @Transactional
    public Medicamento save(Medicamento medicamento) {
        if (medicamento.getId() == null) {
            medicamento.atribuirId(UUID.randomUUID());
        }
        MedicamentoJpaEntity salvo =
            jpaRepository.save(MedicamentoPersistenceMapper.toJpa(medicamento));
        salvarControladoSePresente(medicamento, salvo.getId());
        return hidratar(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Medicamento> findById(UUID id) {
        return jpaRepository.findById(id).map(this::hidratar);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Medicamento> findByNomeComercial(String nome) {
        return jpaRepository.findFirstByNomeComercial(nome).map(this::hidratar);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Medicamento> findByCodigoEan(String ean) {
        return jpaRepository.findByCodigoEan(ean).map(this::hidratar);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Medicamento> findByCodigoAnvisa(String codigo) {
        return jpaRepository.findByCodigoAnvisa(codigo).map(this::hidratar);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicamento> findAll() {
        return jpaRepository.findAll().stream()
            .map(this::hidratar)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicamento> findAllOrdenados(int offset, int limit, String busca) {
        String filtro = normalizarFiltro(busca);
        PageRequest page = pageRequest(offset, limit);
        return pageToList(filtro == null
            ? jpaRepository.findAllByOrderByNomeComercialAsc(page)
            : jpaRepository.buscarOrdenados(filtro, page));
    }

    @Override
    @Transactional(readOnly = true)
    public long contar(String busca) {
        String filtro = normalizarFiltro(busca);
        return (filtro == null
            ? jpaRepository.findAllByOrderByNomeComercialAsc(PageRequest.of(0, 1))
            : jpaRepository.buscarOrdenados(filtro, PageRequest.of(0, 1)))
            .getTotalElements();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Medicamento> findAtivosOrdenados(int offset, int limit, String nomeFiltro) {
        String filtro = normalizarFiltro(nomeFiltro);
        PageRequest page = pageRequest(offset, limit);
        return pageToList(filtro == null
            ? jpaRepository.findByAtivoTrueOrderByNomeComercialAsc(page)
            : jpaRepository.buscarAtivosOrdenados(filtro, page));
    }

    @Override
    @Transactional(readOnly = true)
    public long contarAtivos(String nomeFiltro) {
        String filtro = normalizarFiltro(nomeFiltro);
        return (filtro == null
            ? jpaRepository.findByAtivoTrueOrderByNomeComercialAsc(PageRequest.of(0, 1))
            : jpaRepository.buscarAtivosOrdenados(filtro, PageRequest.of(0, 1)))
            .getTotalElements();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCodigoEan(String ean) {
        return jpaRepository.existsByCodigoEan(ean);
    }

    @Override
    @Transactional(readOnly = true)
    public Medicamento getReferenceById(UUID id) {
        return hidratar(jpaRepository.getReferenceById(id));
    }

    private PageRequest pageRequest(int offset, int limit) {
        int size = Math.max(limit, 1);
        return PageRequest.of(offset / size, size, Sort.by("nomeComercial").ascending());
    }

    private List<Medicamento> pageToList(org.springframework.data.domain.Page<MedicamentoJpaEntity> page) {
        return page.stream().map(this::hidratar).toList();
    }

    private String normalizarFiltro(String nomeFiltro) {
        if (nomeFiltro == null || nomeFiltro.isBlank()) {
            return null;
        }
        return nomeFiltro.trim();
    }

    /**
     * Reconstrói o agregado carregando as referências de fabricante e categoria
     * a partir das respectivas tabelas (id-only quando não encontradas).
     */
    private Medicamento hidratar(MedicamentoJpaEntity e) {
        Fabricante fabricante = e.getFabricanteId() == null ? null
            : fabricanteJpaRepository.findById(e.getFabricanteId())
                .map(MedicamentoPersistenceMapper::toDomain)
                .orElseGet(() -> Fabricante.builder().id(e.getFabricanteId()).build());

        Categoria categoria = e.getCategoriaId() == null ? null
            : categoriaJpaRepository.findById(e.getCategoriaId())
                .map(MedicamentoPersistenceMapper::toDomain)
                .orElseGet(() -> Categoria.builder().id(e.getCategoriaId()).build());

        MedicamentoControlado controlado = controladoJpaRepository.findByMedicamentoId(e.getId())
            .map(c -> MedicamentoControladoPersistenceMapper.toDomain(c,
                Medicamento.builder().id(e.getId()).build()))
            .orElse(null);

        return MedicamentoPersistenceMapper.toDomain(e, fabricante, categoria, controlado);
    }

    private void salvarControladoSePresente(Medicamento medicamento, UUID medicamentoId) {
        MedicamentoControlado ctrl = medicamento.getMedicamentoControlado();
        if (ctrl == null) {
            return;
        }
        if (ctrl.getId() == null) {
            ctrl.atribuirId(java.util.UUID.randomUUID());
        }
        ctrl.vincularMedicamento(Medicamento.builder().id(medicamentoId).build());
        controladoJpaRepository.findByMedicamentoId(medicamentoId)
            .ifPresentOrElse(
                existente -> {
                    existente.setPortaria(ctrl.getPortaria());
                    existente.setLista(ctrl.getLista());
                    existente.setQuantidadeMaximaReceita(ctrl.getQuantidadeMaximaReceita());
                    existente.setValidadeReceitaDias(ctrl.getValidadeReceitaDias());
                    existente.setPsicootropico(ctrl.getPsicootropico());
                    existente.setEntorpecente(ctrl.getEntorpecente());
                    controladoJpaRepository.save(existente);
                },
                () -> controladoJpaRepository.save(
                    MedicamentoControladoPersistenceMapper.toJpa(ctrl, medicamentoId))
            );
    }
}
