package br.com.farmacia.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração de segurança do Sistema Farmacêutico.
 *
 * <p>Hierarquia de roles (menor para maior privilégio):</p>
 * <ul>
 *   <li>{@code ROLE_BALCONISTA}    — Atendimento e PDV</li>
 *   <li>{@code ROLE_ESTOQUISTA}    — Estoque e recebimento</li>
 *   <li>{@code ROLE_FARMACEUTICO}  — Valida receitas, libera controlados</li>
 *   <li>{@code ROLE_GERENTE}       — Relatórios, descontos, fechamento de caixa</li>
 *   <li>{@code ROLE_ADMIN}         — Acesso total + configurações</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)   // habilita @PreAuthorize nos use cases
public class SecurityConfig {

    @Value("${app.security.swagger-public:false}")
    private boolean swaggerPublic;

    @Value("${app.cors.allowed-origin-patterns}")
    private String corsAllowedOriginPatterns;

    // ─── Filter Chain principal ───────────────────────────────────────────────

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
            // Desabilita CSRF (API stateless com JWT não precisa)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless — sem sessão HTTP
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Headers de segurança
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                .referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN))
            )

            // Regras de autorização por endpoint
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                if (swaggerPublic) {
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll();
                } else {
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .denyAll();
                }
                auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll();
                auth.requestMatchers(HttpMethod.GET, "/api/v1/auth/contexto")
                    .hasAnyRole("BALCONISTA", "ESTOQUISTA", "FARMACEUTICO", "GERENTE", "ADMIN");

                auth.requestMatchers(HttpMethod.GET, "/api/v1/medicamentos/**")
                    .hasAnyRole("BALCONISTA", "ESTOQUISTA", "FARMACEUTICO", "GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.POST, "/api/v1/medicamentos/**")
                    .hasAnyRole("GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.PUT, "/api/v1/medicamentos/**")
                    .hasAnyRole("GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.DELETE, "/api/v1/medicamentos/**")
                    .hasRole("ADMIN");

                // ── Estoque ─────────────────────────────────────────────────
                auth.requestMatchers(HttpMethod.GET, "/api/v1/estoque/**")
                    .hasAnyRole("BALCONISTA", "ESTOQUISTA", "FARMACEUTICO", "GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.POST, "/api/v1/estoque/entrada/**")
                    .hasAnyRole("ESTOQUISTA", "GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.PUT, "/api/v1/estoque/medicamentos/*/parametros")
                    .hasAnyRole("ESTOQUISTA", "GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.POST, "/api/v1/estoque/ajuste/**")
                    .hasAnyRole("GERENTE", "ADMIN");

                auth.requestMatchers("/api/v1/vendas/**")
                    .hasAnyRole("BALCONISTA", "FARMACEUTICO", "GERENTE", "ADMIN");
                auth.requestMatchers("/api/v1/pdv/**")
                    .hasAnyRole("BALCONISTA", "FARMACEUTICO", "GERENTE", "ADMIN");

                auth.requestMatchers(HttpMethod.GET, "/api/v1/receitas/**")
                    .hasAnyRole("BALCONISTA", "FARMACEUTICO", "GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.POST, "/api/v1/receitas/**")
                    .hasAnyRole("BALCONISTA", "FARMACEUTICO", "GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.PUT, "/api/v1/receitas/*/validar")
                    .hasAnyRole("FARMACEUTICO", "ADMIN"); // H-09: GERENTE removido — validação é atribuição técnica exclusiva do farmacêutico
                auth.requestMatchers(HttpMethod.PUT, "/api/v1/receitas/*/rejeitar")
                    .hasAnyRole("FARMACEUTICO", "ADMIN"); // H-09: rejeição de receita exige habilitação técnica do farmacêutico

                auth.requestMatchers("/api/v1/sngpc/**")
                    .hasAnyRole("FARMACEUTICO", "GERENTE", "ADMIN");

                auth.requestMatchers("/api/v1/fornecedores/**")
                    .hasAnyRole("ESTOQUISTA", "GERENTE", "ADMIN");
                auth.requestMatchers("/api/v1/compras/**")
                    .hasAnyRole("ESTOQUISTA", "GERENTE", "ADMIN");

                auth.requestMatchers(HttpMethod.GET, "/api/v1/catalogo/**")
                    .hasAnyRole("BALCONISTA", "ESTOQUISTA", "FARMACEUTICO", "GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.POST, "/api/v1/catalogo/fabricantes/**",
                                 "/api/v1/catalogo/categorias/**")
                    .hasAnyRole("GERENTE", "ADMIN");
                auth.requestMatchers(HttpMethod.POST, "/api/v1/catalogo/prescritores/**")
                    .hasAnyRole("BALCONISTA", "FARMACEUTICO", "GERENTE", "ADMIN");

                auth.requestMatchers("/api/v1/clientes/**")
                    .hasAnyRole("BALCONISTA", "FARMACEUTICO", "GERENTE", "ADMIN");

                auth.requestMatchers("/api/v1/caixa/**")
                    .hasAnyRole("BALCONISTA", "FARMACEUTICO", "GERENTE", "ADMIN");
                auth.requestMatchers("/api/v1/financeiro/**")
                    .hasAnyRole("GERENTE", "ADMIN");

                auth.requestMatchers("/api/v1/relatorios/**")
                    .hasAnyRole("GERENTE", "ADMIN");

                auth.requestMatchers("/api/v1/funcionarios/**").hasRole("ADMIN");
                auth.requestMatchers("/api/v1/configuracoes/**").hasRole("ADMIN");
                auth.requestMatchers("/actuator/**").hasRole("ADMIN");

                auth.anyRequest().authenticated();
            })

            // Resource server com JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    /**
     * Converte o claim "roles" do JWT para as authorities do Spring Security.
     * O JWT deve ter: {@code { "roles": ["ROLE_BALCONISTA", "ROLE_FARMACEUTICO"] }}
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("");   // já vem com ROLE_ no token

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        converter.setPrincipalClaimName("sub"); // sub = email do funcionário
        return converter;
    }

    // ─── Password Encoder ─────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // custo 12 para produção
    }

    // ─── CORS ─────────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsAllowedOriginPatterns.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
        config.setAllowedOriginPatterns(origins.isEmpty()
            ? List.of("http://localhost:*")
            : origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
