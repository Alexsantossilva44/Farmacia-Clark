package br.com.farmacia.infrastructure.security;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Carrega chaves RSA a partir de PEM inline ou de um {@link Resource} Spring.
 *
 * @author Alex Silva e Claude
 */
public final class JwtRsaKeyLoader {

    private JwtRsaKeyLoader() {
    }

    public static RSAPrivateKey loadPrivateKey(JwtKeyProperties properties)
            throws IOException, GeneralSecurityException {
        String pem = resolvePem(properties.getPrivateKey(), properties.getPrivateKeyLocation(), "privada");
        return parsePrivateKey(pem);
    }

    public static RSAPublicKey loadPublicKey(JwtKeyProperties properties)
            throws IOException, GeneralSecurityException {
        String pem = resolvePem(properties.getPublicKey(), properties.getPublicKeyLocation(), "pública");
        return parsePublicKey(pem);
    }

    private static String resolvePem(String inlinePem, String location, String tipo)
            throws IOException {
        if (inlinePem != null && !inlinePem.isBlank()) {
            return normalizePem(inlinePem);
        }
        if (location == null || location.isBlank()) {
            throw new IllegalStateException(
                "Chave JWT " + tipo + " não configurada: defina PEM inline ou *_KEY_LOCATION.");
        }
        Resource resource = new DefaultResourceLoader().getResource(location);
        if (!resource.exists()) {
            throw new IllegalStateException("Recurso de chave JWT " + tipo + " não encontrado: " + location);
        }
        try (InputStream input = resource.getInputStream()) {
            return normalizePem(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    static String normalizePem(String pem) {
        return pem.replace("\\n", "\n").trim();
    }

    static RSAPrivateKey parsePrivateKey(String pem) throws GeneralSecurityException {
        byte[] decoded = decodePemBlock(pem, "PRIVATE KEY");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    static RSAPublicKey parsePublicKey(String pem) throws GeneralSecurityException {
        byte[] decoded = decodePemBlock(pem, "PUBLIC KEY");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static byte[] decodePemBlock(String pem, String label) {
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        if (!pem.contains(begin)) {
            throw new IllegalArgumentException("PEM inválido: cabeçalho " + begin + " ausente.");
        }
        String base64 = pem
            .replace(begin, "")
            .replace(end, "")
            .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
