package br.com.farmacia.qa.seed;

import java.util.UUID;

/**
 * Identificadores fixos do seed de integração/testes.
 *
 * <p>Centraliza os UUIDs de referência entre agregados para que testes,
 * BDD e (futuro) front-end consumam os mesmos vínculos FK estáveis.
 * Espelhe estes ids em mocks do front-end ou em {@code application-dev.yml}
 * quando houver ambiente de desenvolvimento compartilhado.</p>
 *
 * @author Alex Silva e Claude
 */
public final class IntegracaoSeedReferencia {

    private IntegracaoSeedReferencia() {
    }

    // ── Catálogo (V1) ─────────────────────────────────────────────────────
    public static final UUID FABRICANTE_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID CATEGORIA_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID MEDICAMENTO_DIPIRONA_ID =
        UUID.fromString("33333333-3333-3333-3333-333333333333");

    // ── Operacional (V5) ──────────────────────────────────────────────────
    public static final UUID PDV_01_ID =
        UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final String PDV_01_NUMERO = "PDV-01";

    // ── Credenciais de teste (semear via IntegracaoTestSeed) ───────────────
    public static final String ADMIN_EMAIL     = "admin@farmacia.com";
    public static final String ADMIN_SENHA     = "admin123";
    public static final String BALCONISTA_EMAIL = "balconista@farmacia.com";
    public static final String BALCONISTA_SENHA = "bal123";
    public static final String FARMACEUTICO_EMAIL = "farmaceutico@farmacia.com";
    public static final String FARMACEUTICO_SENHA = "farm123";
    public static final String FARMACEUTICO_CRF   = "CRF-12345/SP";
    /** Alinhados a DevAmbienteSeed e LoginPage — papéis GERENTE e ESTOQUISTA (RoleSistema). */
    public static final String GERENTE_EMAIL      = "gerente@farmacia.com";
    public static final String GERENTE_SENHA      = "ger123";
    public static final String ESTOQUISTA_EMAIL   = "estoquista@farmacia.com";
    public static final String ESTOQUISTA_SENHA   = "est123";
}
