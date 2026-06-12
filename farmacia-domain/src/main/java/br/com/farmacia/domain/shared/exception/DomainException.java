package br.com.farmacia.domain.shared.exception;

/**
 * Exceção base para violações de regras de negócio do domínio.
 *
 * @author Alex Silva e Claude
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
