package br.com.farmacia.application.compra.usecase;

import br.com.farmacia.domain.compra.entity.Fornecedor;
import br.com.farmacia.domain.compra.repository.FornecedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarFornecedoresUseCase {

    private final FornecedorRepository fornecedorRepository;

    @Transactional(readOnly = true)
    public List<Fornecedor> executar() {
        return fornecedorRepository.findAllAtivos();
    }
}
