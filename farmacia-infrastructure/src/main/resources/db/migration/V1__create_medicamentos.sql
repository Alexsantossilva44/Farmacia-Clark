-- ============================================================
-- V1 — Catálogo de Medicamentos
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Fabricantes ──────────────────────────────────────────────
CREATE TABLE fabricantes (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    razao_social          VARCHAR(150) NOT NULL,
    nome_fantasia         VARCHAR(150),
    cnpj                  CHAR(14)    NOT NULL UNIQUE,
    autorizacao_anvisa    VARCHAR(30),
    email                 VARCHAR(120),
    telefone              VARCHAR(20),
    ativo                 BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Categorias (auto-referenciada para hierarquia) ───────────
CREATE TABLE categorias (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    nome              VARCHAR(100) NOT NULL,
    descricao         TEXT,
    categoria_pai_id  UUID        REFERENCES categorias(id),
    ativo             BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Princípios Ativos ────────────────────────────────────────
CREATE TABLE principios_ativos (
    id     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    nome   VARCHAR(150) NOT NULL UNIQUE,
    dcb    VARCHAR(10),   -- Denominação Comum Brasileira
    cas    VARCHAR(20)    -- Chemical Abstracts Service
);

-- ── Medicamentos ─────────────────────────────────────────────
CREATE TABLE medicamentos (
    id                        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo_ean                VARCHAR(13)   UNIQUE,
    codigo_anvisa             VARCHAR(15)   UNIQUE,
    nome_comercial            VARCHAR(200)  NOT NULL,
    nome_generico             VARCHAR(200),
    tipo                      VARCHAR(20)   NOT NULL
                              CHECK (tipo IN ('REFERENCIA','GENERICO','SIMILAR',
                                              'BIOLOGICO','FITOTERAPICO','HOMEOPATICO','OTC')),
    forma_farmaceutica        VARCHAR(30)
                              CHECK (forma_farmaceutica IN ('COMPRIMIDO','CAPSULA','XAROPE',
                                    'SOLUCAO','SUSPENSAO','INJETAVEL','POMADA','CREME',
                                    'GEL','SUPOSITORIO','COLIRIO','SPRAY','PATCH','PO')),
    concentracao              VARCHAR(50),
    apresentacao              VARCHAR(100),
    classe_terapeutica        VARCHAR(100),
    requer_receita            BOOLEAN       NOT NULL DEFAULT FALSE,
    nivel_controle            VARCHAR(25)   NOT NULL DEFAULT 'LIVRE'
                              CHECK (nivel_controle IN ('LIVRE','RECEITA_SIMPLES',
                                    'CONTROLADO_C1','CONTROLADO_C2','CONTROLADO_B1',
                                    'CONTROLADO_B2','ANTIMICROBIANO')),
    preco_maximo_consumidor   NUMERIC(10,2),
    fabricante_id             UUID          REFERENCES fabricantes(id),
    categoria_id              UUID          REFERENCES categorias(id),
    ativo                     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Associação Medicamento ↔ Princípio Ativo ─────────────────
CREATE TABLE medicamento_principio_ativo (
    medicamento_id    UUID NOT NULL REFERENCES medicamentos(id) ON DELETE CASCADE,
    principio_ativo_id UUID NOT NULL REFERENCES principios_ativos(id),
    PRIMARY KEY (medicamento_id, principio_ativo_id)
);

-- ── Medicamentos Controlados (extensão) ──────────────────────
CREATE TABLE medicamentos_controlados (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    medicamento_id              UUID        NOT NULL UNIQUE REFERENCES medicamentos(id),
    portaria                    VARCHAR(30) NOT NULL,   -- ex: 'Portaria 344/98'
    lista                       VARCHAR(10) NOT NULL,   -- ex: 'B1', 'C1', 'C2'
    quantidade_maxima_receita   INTEGER     NOT NULL DEFAULT 1,
    validade_receita_dias       INTEGER     NOT NULL DEFAULT 30,
    psicootropico               BOOLEAN     NOT NULL DEFAULT FALSE,
    entorpecente                BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Índices ───────────────────────────────────────────────────
CREATE INDEX idx_medicamentos_nome_comercial ON medicamentos(nome_comercial);
CREATE INDEX idx_medicamentos_codigo_anvisa  ON medicamentos(codigo_anvisa);
CREATE INDEX idx_medicamentos_nivel_controle ON medicamentos(nivel_controle);
CREATE INDEX idx_medicamentos_ativo          ON medicamentos(ativo);
