package br.com.farmacia.application.compra.usecase;

import br.com.farmacia.domain.compra.entity.NotaFiscalEntrada;
import br.com.farmacia.domain.compra.repository.NotaFiscalEntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListarNotasFiscaisEntradaUseCase {

    private final NotaFiscalEntradaRepository notaFiscalRepository;

    @Transactional(readOnly = true)
    public List<NotaFiscalEntrada> executar() {
        return notaFiscalRepository.findAllOrderByDataEntradaDesc();
    }
}
