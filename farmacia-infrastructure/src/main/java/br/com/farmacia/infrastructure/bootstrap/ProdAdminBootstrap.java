package br.com.farmacia.infrastructure.bootstrap;

import br.com.farmacia.domain.funcionario.entity.Cargo;
import br.com.farmacia.domain.funcionario.entity.Funcionario;
import br.com.farmacia.domain.funcionario.enums.RoleSistema;
import br.com.farmacia.domain.funcionario.repository.FuncionarioRepository;
import br.com.farmacia.infrastructure.persistence.funcionario.CargoJpaRepository;
import br.com.farmacia.infrastructure.persistence.funcionario.FuncionarioJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Cria o primeiro administrador em produção quando o banco está vazio.
 * Defina {@code APP_BOOTSTRAP_ADMIN_EMAIL} e {@code APP_BOOTSTRAP_ADMIN_PASSWORD} no deploy.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdAdminBootstrap implements ApplicationRunner {

    @Value("${app.bootstrap.admin-email}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password}")
    private String adminPassword;

    @Value("${app.bootstrap.admin-nome:Administrador}")
    private String adminNome;

    private final FuncionarioRepository funcionarioRepository;
    private final FuncionarioJpaRepository funcionarioJpaRepository;
    private final CargoJpaRepository cargoJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("[prod] Bootstrap de admin não configurado (defina APP_BOOTSTRAP_ADMIN_EMAIL e senha).");
            return;
        }
        if (funcionarioRepository.findByEmail(adminEmail.trim().toLowerCase()).isPresent()) {
            return;
        }
        if (funcionarioJpaRepository.count() > 0) {
            log.warn("[prod] Bootstrap de admin ignorado: já existem funcionários cadastrados.");
            return;
        }

        var cargoJpa = cargoJpaRepository.findByRoleSistema(RoleSistema.ROLE_ADMIN)
            .orElseThrow(() -> new IllegalStateException(
                "Cargo ROLE_ADMIN não encontrado (verifique migration V3)."));

        funcionarioRepository.save(Funcionario.builder()
            .nome(adminNome)
            .cpf("00000000000")
            .email(adminEmail.trim().toLowerCase())
            .senhaHash(passwordEncoder.encode(adminPassword))
            .cargo(Cargo.builder()
                .id(cargoJpa.getId())
                .roleSistema(RoleSistema.ROLE_ADMIN)
                .build())
            .dataAdmissao(LocalDate.now())
            .ativo(true)
            .build());

        log.info("[prod] Administrador inicial criado para {}", adminEmail);
    }
}
