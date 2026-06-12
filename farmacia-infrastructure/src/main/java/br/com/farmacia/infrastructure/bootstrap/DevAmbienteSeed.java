package br.com.farmacia.infrastructure.bootstrap;

import br.com.farmacia.domain.funcionario.entity.Cargo;
import br.com.farmacia.domain.funcionario.entity.Farmaceutico;
import br.com.farmacia.domain.funcionario.entity.Funcionario;
import br.com.farmacia.domain.funcionario.enums.RoleSistema;
import br.com.farmacia.domain.funcionario.repository.FarmaceuticoRepository;
import br.com.farmacia.domain.funcionario.repository.FuncionarioRepository;
import br.com.farmacia.infrastructure.persistence.funcionario.CargoJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Seed idempotente de funcionários para desenvolvimento local.
 *
 * <p>As credenciais espelham {@code IntegracaoSeedReferencia} (testes/BDD) e o
 * README, permitindo login via {@code POST /api/v1/auth/token} após subir o
 * Docker Compose.</p>
 *
 * <p>Alteração: incluídos {@code GERENTE} e {@code ESTOQUISTA} — os 5 papéis
 * definidos em {@code RoleSistema} / migration V3, alinhados aos atalhos da
 * {@code LoginPage} e às permissões do front ({@code auth.ts}).</p>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevAmbienteSeed implements ApplicationRunner {

    public static final String ADMIN_EMAIL = "admin@farmacia.com";
    public static final String ADMIN_SENHA = "admin123";
    public static final String BALCONISTA_EMAIL = "balconista@farmacia.com";
    public static final String BALCONISTA_SENHA = "bal123";
    public static final String FARMACEUTICO_EMAIL = "farmaceutico@farmacia.com";
    public static final String FARMACEUTICO_SENHA = "farm123";
    public static final String FARMACEUTICO_CRF = "CRF-12345/SP";
    /** Papéis adicionados para espelhar RoleSistema (GERENTE, ESTOQUISTA) no ambiente dev. */
    public static final String GERENTE_EMAIL = "gerente@farmacia.com";
    public static final String GERENTE_SENHA = "ger123";
    public static final String ESTOQUISTA_EMAIL = "estoquista@farmacia.com";
    public static final String ESTOQUISTA_SENHA = "est123";

    private final FuncionarioRepository funcionarioRepository;
    private final FarmaceuticoRepository farmaceuticoRepository;
    private final CargoJpaRepository cargoJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        semearFuncionario(ADMIN_EMAIL, ADMIN_SENHA, RoleSistema.ROLE_ADMIN,
            "Administrador Dev", "00000000001");
        semearFuncionario(BALCONISTA_EMAIL, BALCONISTA_SENHA, RoleSistema.ROLE_BALCONISTA,
            "Balconista Dev", "00000000002");
        semearFuncionario(FARMACEUTICO_EMAIL, FARMACEUTICO_SENHA, RoleSistema.ROLE_FARMACEUTICO,
            "Farmacêutico Dev", "00000000003");
        // Gerente e Estoquista — permissões de estoque/compras e cadastros no front.
        semearFuncionario(GERENTE_EMAIL, GERENTE_SENHA, RoleSistema.ROLE_GERENTE,
            "Gerente Dev", "00000000004");
        semearFuncionario(ESTOQUISTA_EMAIL, ESTOQUISTA_SENHA, RoleSistema.ROLE_ESTOQUISTA,
            "Estoquista Dev", "00000000005");
        semearFarmaceutico();

        log.info("""
            [dev] Usuários de desenvolvimento disponíveis:
              admin@farmacia.com / admin123
              gerente@farmacia.com / ger123
              farmaceutico@farmacia.com / farm123
              estoquista@farmacia.com / est123
              balconista@farmacia.com / bal123
            """);
    }

    private void semearFuncionario(String email, String senha, RoleSistema role,
                                   String nome, String cpf) {
        if (funcionarioRepository.findByEmail(email).isPresent()) {
            return;
        }
        var cargoJpa = cargoJpaRepository.findByRoleSistema(role)
            .orElseThrow(() -> new IllegalStateException(
                "Cargo não encontrado para " + role + " (migration V3)"));

        funcionarioRepository.save(Funcionario.builder()
            .nome(nome)
            .cpf(cpf)
            .email(email)
            .senhaHash(passwordEncoder.encode(senha))
            .cargo(Cargo.builder()
                .id(cargoJpa.getId())
                .roleSistema(role)
                .build())
            .dataAdmissao(LocalDate.now())
            .ativo(true)
            .build());

        log.info("[dev] Funcionário criado: {}", email);
    }

    private void semearFarmaceutico() {
        var funcionario = funcionarioRepository.findByEmail(FARMACEUTICO_EMAIL).orElseThrow();
        if (farmaceuticoRepository.findByFuncionarioId(funcionario.getId()).isPresent()) {
            return;
        }
        farmaceuticoRepository.save(Farmaceutico.builder()
            .funcionario(funcionario)
            .crf(FARMACEUTICO_CRF)
            .ufCrf("SP")
            .responsavelTecnico(true)
            .ativo(true)
            .build());
    }
}
