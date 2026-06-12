package br.com.farmacia.infrastructure.security;

import br.com.farmacia.domain.funcionario.entity.Funcionario;
import br.com.farmacia.domain.funcionario.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Serviço de autenticação e emissão de tokens JWT.
 *
 * <p>Verifica as credenciais do funcionário (e-mail + senha BCrypt) contra a
 * porta de domínio {@link FuncionarioRepository} e, em caso de sucesso, emite
 * um JWT RS256 contendo o claim {@code roles} compreendido pelo
 * {@code SecurityConfig} (resource server).</p>
 *
 * <p>Mantido na camada de infraestrutura por depender de detalhes técnicos de
 * segurança ({@link PasswordEncoder} e {@link JwtEncoder}); o domínio
 * permanece livre desses frameworks.</p>
 *
 * @author Alex Silva e Claude
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${app.security.jwt.expiration-seconds:28800}")
    private long expiracaoSegundos;

    @Value("${app.security.jwt.issuer:farmacia}")
    private String issuer;

    /**
     * Autentica o funcionário e gera o token de acesso.
     *
     * @throws CredenciaisInvalidasException se o e-mail não existir, o
     *         funcionário estiver inativo ou a senha não conferir.
     */
    @Transactional(readOnly = true)
    public TokenResult autenticar(String email, String senhaPura) {
        Funcionario funcionario = funcionarioRepository.findByEmail(email)
            .filter(f -> Boolean.TRUE.equals(f.getAtivo()))
            .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(senhaPura, funcionario.getSenhaHash())) {
            log.warn("Tentativa de login com senha inválida para {}", email);
            throw new CredenciaisInvalidasException();
        }

        return gerarToken(funcionario);
    }

    private TokenResult gerarToken(Funcionario funcionario) {
        Instant agora = Instant.now();

        String role = funcionario.getCargo() != null
                && funcionario.getCargo().getRoleSistema() != null
            ? funcionario.getCargo().getRoleSistema().name()
            : "ROLE_BALCONISTA";

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(agora)
            .expiresAt(agora.plusSeconds(expiracaoSegundos))
            .subject(funcionario.getEmail())
            .claim("roles", List.of(role))
            .claim("nome", funcionario.getNome())
            .claim("funcionarioId", String.valueOf(funcionario.getId()))
            .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResult(token, "Bearer", expiracaoSegundos);
    }

    /** Resultado da autenticação: token assinado e metadados. */
    public record TokenResult(String token, String tipo, long expiraEmSegundos) {
    }

    /** Lançada quando as credenciais informadas são inválidas. */
    public static class CredenciaisInvalidasException extends RuntimeException {
        public CredenciaisInvalidasException() {
            super("E-mail ou senha inválidos");
        }
    }
}
