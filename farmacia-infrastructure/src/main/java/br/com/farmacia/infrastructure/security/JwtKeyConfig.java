package br.com.farmacia.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Beans JWT RS256: apenas a API de autenticação usa a chave privada;
 * validadores (resource server) usam somente a chave pública.
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(JwtKeyProperties.class)
public class JwtKeyConfig {

    @Bean
    public RSAPrivateKey jwtSigningKey(JwtKeyProperties properties) throws Exception {
        RSAPrivateKey key = JwtRsaKeyLoader.loadPrivateKey(properties);
        log.info("JWT RS256: chave privada carregada ({} bits)", key.getModulus().bitLength());
        return key;
    }

    @Bean
    public RSAPublicKey jwtVerificationKey(JwtKeyProperties properties) throws Exception {
        RSAPublicKey key = JwtRsaKeyLoader.loadPublicKey(properties);
        log.info("JWT RS256: chave pública carregada ({} bits)", key.getModulus().bitLength());
        return key;
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAPrivateKey jwtSigningKey, RSAPublicKey jwtVerificationKey) {
        RSAKey rsaJwk = new RSAKey.Builder(jwtVerificationKey)
            .privateKey(jwtSigningKey)
            .keyID("farmacia-jwt-rs256")
            .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaJwk)));
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey jwtVerificationKey) {
        return NimbusJwtDecoder.withPublicKey(jwtVerificationKey).build();
    }
}
