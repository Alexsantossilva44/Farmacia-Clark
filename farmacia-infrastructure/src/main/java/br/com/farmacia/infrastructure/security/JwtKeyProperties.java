package br.com.farmacia.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração das chaves RSA usadas para assinar (privada) e validar (pública) JWT RS256.
 *
 * @author Alex Silva e Claude
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtKeyProperties {

    /** PEM inline (prioridade sobre {@link #privateKeyLocation}). */
    private String privateKey = "";

    /** PEM inline (prioridade sobre {@link #publicKeyLocation}). */
    private String publicKey = "";

    /** Ex.: {@code classpath:jwt/dev-private.pem} ou {@code file:/app/secrets/jwt-private.pem}. */
    private String privateKeyLocation = "classpath:jwt/dev-private.pem";

    /** Ex.: {@code classpath:jwt/dev-public.pem} ou {@code file:/app/secrets/jwt-public.pem}. */
    private String publicKeyLocation = "classpath:jwt/dev-public.pem";
}
