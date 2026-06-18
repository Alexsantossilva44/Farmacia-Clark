package br.com.farmacia.api.v1.controller;

import br.com.farmacia.api.v1.model.input.CategoriaInput;
import br.com.farmacia.api.v1.model.input.FabricanteInput;
import br.com.farmacia.api.v1.model.input.PrescritorInput;
import br.com.farmacia.domain.compra.exception.CnpjDuplicadoException;
import br.com.farmacia.domain.compra.exception.CnpjInvalidoException;
import br.com.farmacia.domain.medicamento.exception.FabricanteDuplicadoException;
import br.com.farmacia.api.v1.model.output.CatalogoModels.CategoriaModel;
import br.com.farmacia.api.v1.model.output.CatalogoModels.FabricanteModel;
import br.com.farmacia.api.v1.model.output.CatalogoModels.PrescritorModel;
import br.com.farmacia.infrastructure.persistence.medicamento.CategoriaJpaEntity;
import br.com.farmacia.infrastructure.persistence.medicamento.CategoriaJpaRepository;
import br.com.farmacia.infrastructure.persistence.medicamento.FabricanteJpaEntity;
import br.com.farmacia.infrastructure.persistence.medicamento.FabricanteJpaRepository;
import br.com.farmacia.infrastructure.persistence.receituario.PrescritorJpaEntity;
import br.com.farmacia.infrastructure.persistence.receituario.PrescritorJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints auxiliares para cadastros de referência (fabricantes, categorias, prescritores).
 * Usado pelo front-end Farmácia Clark nos formulários de medicamento e receituário.
 */
@RestController
@RequestMapping(path = "/api/v1/catalogo", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Catálogo auxiliar", description = "Fabricantes, categorias e prescritores")
@SecurityRequirement(name = "bearerAuth")
public class CatalogoController {

    private final FabricanteJpaRepository fabricanteRepository;
    private final CategoriaJpaRepository categoriaRepository;
    private final PrescritorJpaRepository prescritorRepository;

    // ─── Fabricantes ──────────────────────────────────────────────────────────

    @GetMapping("/fabricantes")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Listar fabricantes ativos")
    public List<FabricanteModel> listarFabricantes() {
        return fabricanteRepository.findAll(Sort.by("razaoSocial").ascending()).stream()
            .filter(f -> f.getAtivo() == null || Boolean.TRUE.equals(f.getAtivo()))
            .map(this::toFabricanteModel)
            .toList();
    }

    @PostMapping("/fabricantes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Cadastrar fabricante")
    public FabricanteModel cadastrarFabricante(@RequestBody @Valid FabricanteInput input) {
        String cnpj = normalizarCnpj(input.getCnpj());
        if (cnpj.length() != 14) {
            throw new CnpjInvalidoException();
        }
        if (fabricanteRepository.existsByCnpj(cnpj)) {
            throw new CnpjDuplicadoException(input.getCnpj());
        }
        String razaoSocial = input.getRazaoSocial().trim();
        if (fabricanteRepository.countByRazaoSocialAtivo(razaoSocial) > 0) {
            throw new FabricanteDuplicadoException(razaoSocial);
        }
        var entity = FabricanteJpaEntity.builder()
            .id(UUID.randomUUID())
            .razaoSocial(razaoSocial)
            .nomeFantasia(input.getNomeFantasia() != null ? input.getNomeFantasia().trim() : null)
            .cnpj(cnpj)
            .ativo(true)
            .build();
        return toFabricanteModel(fabricanteRepository.save(entity));
    }

    /** Remove formatação da máscara (00.000.000/0000-00) enviada pelo front. */
    private static String normalizarCnpj(String cnpj) {
        return cnpj != null ? cnpj.replaceAll("\\D", "") : "";
    }

    // ─── Categorias ───────────────────────────────────────────────────────────

    @GetMapping("/categorias")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'ESTOQUISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Listar categorias ativas")
    public List<CategoriaModel> listarCategorias() {
        return categoriaRepository.findAll(Sort.by("nome").ascending()).stream()
            .filter(c -> c.getAtivo() == null || Boolean.TRUE.equals(c.getAtivo()))
            .map(this::toCategoriaModel)
            .toList();
    }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Cadastrar categoria")
    public CategoriaModel cadastrarCategoria(@RequestBody @Valid CategoriaInput input) {
        var entity = CategoriaJpaEntity.builder()
            .id(UUID.randomUUID())
            .nome(input.getNome().trim())
            .descricao(input.getDescricao())
            .ativo(true)
            .build();
        return toCategoriaModel(categoriaRepository.save(entity));
    }

    @PutMapping("/categorias/{id}")
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Atualizar categoria")
    public CategoriaModel atualizarCategoria(@PathVariable UUID id, @RequestBody CategoriaInput input) {
        if (input.getNome() == null || input.getNome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Nome da categoria é obrigatório");
        }
        var entity = categoriaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));
        entity.setNome(input.getNome().trim());
        String desc = input.getDescricao();
        entity.setDescricao(desc != null && !desc.isBlank() ? desc.trim() : null);
        return toCategoriaModel(categoriaRepository.save(entity));
    }

    @DeleteMapping("/categorias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Inativar categoria (soft delete)")
    public void inativarCategoria(@PathVariable UUID id) {
        var entity = categoriaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));
        entity.setAtivo(false);
        categoriaRepository.save(entity);
    }

    // ─── Prescritores ─────────────────────────────────────────────────────────

    @GetMapping("/prescritores")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Listar prescritores ativos")
    public List<PrescritorModel> listarPrescritores() {
        return prescritorRepository.findAll().stream()
            .filter(p -> p.getAtivo() == null || Boolean.TRUE.equals(p.getAtivo()))
            .sorted(Comparator.comparing(PrescritorJpaEntity::getNome, String.CASE_INSENSITIVE_ORDER))
            .map(this::toPrescritorModel)
            .toList();
    }

    @PostMapping("/prescritores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Cadastrar prescritor")
    public PrescritorModel cadastrarPrescritor(@RequestBody @Valid PrescritorInput input) {
        var entity = PrescritorJpaEntity.builder()
            .id(UUID.randomUUID())
            .nome(input.getNome().trim())
            .crm(input.getCrm().trim())
            .ufCrm(input.getUfCrm().trim().toUpperCase())
            .especialidade(input.getEspecialidade())
            .email(input.getEmail())
            .ativo(true)
            .createdAt(LocalDateTime.now())
            .build();
        return toPrescritorModel(prescritorRepository.save(entity));
    }

    @PutMapping("/prescritores/{id}")
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Atualizar prescritor")
    public PrescritorModel atualizarPrescritor(@PathVariable UUID id, @RequestBody @Valid PrescritorInput input) {
        var entity = prescritorRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescritor não encontrado"));
        entity.setNome(input.getNome().trim());
        entity.setCrm(input.getCrm().trim());
        entity.setUfCrm(input.getUfCrm().trim().toUpperCase());
        entity.setEspecialidade(input.getEspecialidade());
        entity.setEmail(input.getEmail());
        return toPrescritorModel(prescritorRepository.save(entity));
    }

    @DeleteMapping("/prescritores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('BALCONISTA', 'FARMACEUTICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Inativar prescritor (soft delete)")
    public void inativarPrescritor(@PathVariable UUID id) {
        var entity = prescritorRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescritor não encontrado"));
        entity.setAtivo(false);
        prescritorRepository.save(entity);
    }

    private FabricanteModel toFabricanteModel(FabricanteJpaEntity e) {
        var m = new FabricanteModel();
        m.setId(e.getId());
        m.setRazaoSocial(e.getRazaoSocial());
        m.setNomeFantasia(e.getNomeFantasia());
        m.setCnpj(e.getCnpj());
        m.setAtivo(e.getAtivo());
        return m;
    }

    private CategoriaModel toCategoriaModel(CategoriaJpaEntity e) {
        var m = new CategoriaModel();
        m.setId(e.getId());
        m.setNome(e.getNome());
        m.setDescricao(e.getDescricao());
        m.setAtivo(e.getAtivo());
        return m;
    }

    private PrescritorModel toPrescritorModel(PrescritorJpaEntity e) {
        var m = new PrescritorModel();
        m.setId(e.getId());
        m.setNome(e.getNome());
        m.setCrm(e.getCrm());
        m.setUfCrm(e.getUfCrm());
        m.setEspecialidade(e.getEspecialidade());
        m.setEmail(e.getEmail());
        m.setAtivo(e.getAtivo());
        return m;
    }
}
