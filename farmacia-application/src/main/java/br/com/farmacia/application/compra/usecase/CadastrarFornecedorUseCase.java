package br.com.farmacia.application.compra.usecase;

import br.com.farmacia.domain.compra.entity.Fornecedor;
import br.com.farmacia.domain.compra.exception.CnpjDuplicadoException;
import br.com.farmacia.domain.compra.exception.CnpjInvalidoException;
import br.com.farmacia.domain.compra.repository.FornecedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CadastrarFornecedorUseCase {

    private final FornecedorRepository fornecedorRepository;

    @Transactional
    public Fornecedor executar(Input input) {
        String cnpj = normalizarCnpj(input.cnpj());
        if (cnpj.length() != 14) {
            throw new CnpjInvalidoException();
        }
        if (fornecedorRepository.findByCnpj(cnpj).isPresent()) {
            throw new CnpjDuplicadoException(cnpj);
        }
        return fornecedorRepository.save(Fornecedor.builder()
            .razaoSocial(input.razaoSocial().trim())
            .nomeFantasia(input.nomeFantasia() != null ? input.nomeFantasia().trim() : null)
            .cnpj(cnpj)
            .ativo(true)
            .build());
    }

    static String normalizarCnpj(String cnpj) {
        return cnpj != null ? cnpj.replaceAll("\\D", "") : "";
    }

    public record Input(String razaoSocial, String nomeFantasia, String cnpj) {}
}
