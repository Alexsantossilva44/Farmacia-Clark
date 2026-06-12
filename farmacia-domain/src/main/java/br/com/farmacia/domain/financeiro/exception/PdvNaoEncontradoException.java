package br.com.farmacia.domain.financeiro.exception;

import br.com.farmacia.domain.shared.exception.DomainException;

import java.util.UUID;

/**
 * Lançada quando um PDV referenciado não existe no repositório.
 *
 * @author Alex Silva e Claude
 */
public class PdvNaoEncontradoException extends DomainException {

    public PdvNaoEncontradoException(UUID id) {
        super("PDV não encontrado: " + id);
    }

    public PdvNaoEncontradoException(String numero) {
        super("PDV não encontrado: " + numero);
    }
}
