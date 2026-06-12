package br.com.farmacia.infrastructure.persistence.compra;

import br.com.farmacia.domain.compra.entity.Fornecedor;
import br.com.farmacia.domain.compra.repository.FornecedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FornecedorRepositoryAdapter implements FornecedorRepository {

    private final FornecedorJpaRepository jpaRepository;

    @Override
    @Transactional
    public Fornecedor save(Fornecedor fornecedor) {
        if (fornecedor.getId() == null) {
            fornecedor.atribuirId(UUID.randomUUID());
        }
        return toDomain(jpaRepository.save(toJpa(fornecedor)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Fornecedor> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Fornecedor> findByCnpj(String cnpj) {
        return jpaRepository.findByCnpj(normalizarCnpj(cnpj)).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Fornecedor> findAllAtivos() {
        return jpaRepository.findByAtivoTrueOrderByRazaoSocialAsc().stream()
            .map(this::toDomain)
            .toList();
    }

    private Fornecedor toDomain(FornecedorJpaEntity e) {
        return Fornecedor.builder()
            .id(e.getId())
            .razaoSocial(e.getRazaoSocial())
            .nomeFantasia(e.getNomeFantasia())
            .cnpj(e.getCnpj())
            .ativo(e.getAtivo())
            .build();
    }

    private FornecedorJpaEntity toJpa(Fornecedor f) {
        return FornecedorJpaEntity.builder()
            .id(f.getId())
            .razaoSocial(f.getRazaoSocial())
            .nomeFantasia(f.getNomeFantasia())
            .cnpj(normalizarCnpj(f.getCnpj()))
            .ativo(f.getAtivo() != null ? f.getAtivo() : true)
            .build();
    }

    static String normalizarCnpj(String cnpj) {
        return cnpj != null ? cnpj.replaceAll("\\D", "") : "";
    }
}
