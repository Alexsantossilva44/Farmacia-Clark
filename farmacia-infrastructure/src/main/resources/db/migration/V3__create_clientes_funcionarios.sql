-- ============================================================
-- V3 — Clientes, Funcionários e Farmacêuticos
-- ============================================================

-- ── Cargos ────────────────────────────────────────────────────
CREATE TABLE cargos (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    nome         VARCHAR(60) NOT NULL UNIQUE,
    descricao    TEXT,
    -- roles para o Spring Security (ex: ROLE_FARMACEUTICO)
    role_sistema VARCHAR(40) NOT NULL
);

INSERT INTO cargos (nome, descricao, role_sistema) VALUES
    ('Administrador',       'Acesso total ao sistema',                  'ROLE_ADMIN'),
    ('Gerente',             'Gestão operacional e financeira',           'ROLE_GERENTE'),
    ('Farmacêutico',        'Validação de receitas e dispensação',       'ROLE_FARMACEUTICO'),
    ('Balconista',          'Atendimento e operação de PDV',             'ROLE_BALCONISTA'),
    ('Estoquista',          'Controle de estoque e recebimento',         'ROLE_ESTOQUISTA');

-- ── Funcionários ──────────────────────────────────────────────
CREATE TABLE funcionarios (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    nome              VARCHAR(150) NOT NULL,
    cpf               CHAR(11)    NOT NULL UNIQUE,
    email             VARCHAR(120) NOT NULL UNIQUE,
    senha_hash        VARCHAR(255) NOT NULL,
    telefone          VARCHAR(20),
    cargo_id          UUID        NOT NULL REFERENCES cargos(id),
    data_admissao     DATE        NOT NULL DEFAULT CURRENT_DATE,
    data_demissao     DATE,
    ativo             BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Adiciona FK de funcionário na tabela de movimentações (criada na V2)
ALTER TABLE movimentacoes_estoque
    ADD CONSTRAINT fk_mov_funcionario
    FOREIGN KEY (funcionario_id) REFERENCES funcionarios(id);

-- ── Farmacêuticos (extensão de funcionário) ───────────────────
CREATE TABLE farmaceuticos (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    funcionario_id  UUID        NOT NULL UNIQUE REFERENCES funcionarios(id),
    crf             VARCHAR(15) NOT NULL,   -- Conselho Regional de Farmácia
    uf_crf          CHAR(2)     NOT NULL,
    especialidades  TEXT,
    responsavel_tecnico BOOLEAN NOT NULL DEFAULT FALSE
);

-- ── Clientes ──────────────────────────────────────────────────
CREATE TABLE clientes (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    nome             VARCHAR(150) NOT NULL,
    cpf              CHAR(11)    UNIQUE,
    data_nascimento  DATE,
    sexo             CHAR(1)     CHECK (sexo IN ('M', 'F', 'O')),
    telefone         VARCHAR(20),
    email            VARCHAR(120),
    -- endereço embutido
    logradouro       VARCHAR(200),
    numero           VARCHAR(10),
    complemento      VARCHAR(50),
    bairro           VARCHAR(80),
    cidade           VARCHAR(80),
    uf               CHAR(2),
    cep              CHAR(8),
    -- dados de saúde (sensíveis — criptografar em produção)
    alergias         TEXT,
    observacoes      TEXT,
    data_cadastro    TIMESTAMP   NOT NULL DEFAULT NOW(),
    ativo            BOOLEAN     NOT NULL DEFAULT TRUE
);

-- ── Programa de Fidelidade ────────────────────────────────────
CREATE TABLE programa_fidelidade (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id        UUID        NOT NULL UNIQUE REFERENCES clientes(id),
    pontos_acumulados INTEGER     NOT NULL DEFAULT 0,
    nivel             VARCHAR(20) NOT NULL DEFAULT 'BRONZE'
                      CHECK (nivel IN ('BRONZE','PRATA','OURO','DIAMANTE')),
    data_inscricao    DATE        NOT NULL DEFAULT CURRENT_DATE,
    data_expiracao    DATE
);

-- ── Convênios ─────────────────────────────────────────────────
CREATE TABLE convenios (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    nome          VARCHAR(100) NOT NULL,
    cnpj          CHAR(14)    UNIQUE,
    percentual_desconto NUMERIC(5,2) NOT NULL DEFAULT 0,
    ativo         BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE cliente_convenio (
    cliente_id    UUID NOT NULL REFERENCES clientes(id),
    convenio_id   UUID NOT NULL REFERENCES convenios(id),
    numero_cartao VARCHAR(30),
    data_inicio   DATE NOT NULL DEFAULT CURRENT_DATE,
    data_fim      DATE,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (cliente_id, convenio_id)
);

-- ── Índices ───────────────────────────────────────────────────
CREATE INDEX idx_clientes_cpf   ON clientes(cpf);
CREATE INDEX idx_clientes_nome  ON clientes(nome);
CREATE INDEX idx_func_email     ON funcionarios(email);
CREATE INDEX idx_func_cargo_id  ON funcionarios(cargo_id);
