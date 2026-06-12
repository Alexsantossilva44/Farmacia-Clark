package br.com.farmacia.qa.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança mínima para testes de fatia web ({@code @WebMvcTest}).
 *
 * <p>O {@code SecurityConfig} de produção vive no módulo de infraestrutura e não é
 * carregado pelo slice web. Esta config reproduz apenas o necessário para validar
 * autorização nos controllers:</p>
 * <ul>
 *   <li>{@code @EnableMethodSecurity} — habilita os {@code @PreAuthorize} dos controllers</li>
 *   <li>CSRF desabilitado — API stateless (igual à produção)</li>
 *   <li>{@code httpBasic} como entry point — requisições não autenticadas retornam 401</li>
 * </ul>
 *
 * @author Alex Silva e Claude
 */
@TestConfiguration
@EnableMethodSecurity
public class WebMvcSecurityTestConfig {

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
