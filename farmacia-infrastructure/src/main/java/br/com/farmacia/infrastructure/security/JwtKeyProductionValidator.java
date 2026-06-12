package br.com.farmacia.infrastructure.security;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Valida par de chaves RSA em produção: tamanho mínimo, correspondência e ausência de chaves de dev/test.
 *
 * @author Alex Silva e Claude
 */
@Component
@Profile({"prod", "staging", "homolog"}) // M-10: staging e homolog também devem usar chaves RSA de produção
public class JwtKeyProductionValidator {

    private static final int MIN_KEY_BITS = 2048;

    private final JwtKeyProperties properties;
    private final RSAPrivateKey signingKey;
    private final RSAPublicKey verificationKey;

    public JwtKeyProductionValidator(
            JwtKeyProperties properties,
            RSAPrivateKey signingKey,
            RSAPublicKey verificationKey) {
        this.properties = properties;
        this.signingKey = signingKey;
        this.verificationKey = verificationKey;
    }

    @EventListener(ApplicationReadyEvent.class)
    void validar() {
        if (signingKey.getModulus().bitLength() < MIN_KEY_BITS) {
            throw new IllegalStateException(
                "Chave privada JWT deve ter pelo menos " + MIN_KEY_BITS + " bits em produção.");
        }
        if (!signingKey.getModulus().equals(verificationKey.getModulus())) {
            throw new IllegalStateException("Par JWT inválido: chave pública não corresponde à privada.");
        }
        if (usaChaveDesenvolvimento()) {
            throw new IllegalStateException(
                "Produção não pode usar chaves JWT de desenvolvimento ou teste. "
                    + "Monte um par RSA dedicado (ver DEPLOY.md).");
        }
    }

    private boolean usaChaveDesenvolvimento() {
        String privateLocation = properties.getPrivateKeyLocation();
        String publicLocation = properties.getPublicKeyLocation();
        return contemMarcadorDev(privateLocation) || contemMarcadorDev(publicLocation);
    }

    private static boolean contemMarcadorDev(String location) {
        if (location == null || location.isBlank()) {
            return false;
        }
        String lower = location.toLowerCase();
        return lower.contains("dev-private")
            || lower.contains("dev-public")
            || lower.contains("test-private")
            || lower.contains("test-public")
            || lower.contains("/jwt/dev-")
            || lower.contains("/jwt/test-");
    }
}
