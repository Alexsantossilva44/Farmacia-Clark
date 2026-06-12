-- ============================================================
-- V4 — Receituário, Prescritores e SNGPC
-- ============================================================

-- ── Prescritores ──────────────────────────────────────────────
CREATE TABLE prescritores (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    nome          VARCHAR(150) NOT NULL,
    crm           VARCHAR(15) NOT NULL,
    uf_crm        CHAR(2)     NOT NULL,
    especialidade VARCHAR(80),
    email         VARCHAR(120),
    ativo         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (crm, uf_crm)
);

-- ── Receitas ──────────────────────────────────────────────────
CREATE TABLE receitas (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    numero_receita  VARCHAR(30),
    data_emissao    DATE        NOT NULL,
    data_validade   DATE        NOT NULL,
    tipo            VARCHAR(25) NOT NULL
                    CHECK (tipo IN ('SIMPLES','AZUL','AMARELA',
                                   'BRANCA_ESPECIAL','ANTIMICROBIANO')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
                    CHECK (status IN ('PENDENTE','APROVADA','REJEITADA',
                                     'UTILIZADA','VENCIDA','SUSPENSA')),
    cid             VARCHAR(10),
    retida          BOOLEAN     NOT NULL DEFAULT FALSE,
    imagem_path     VARCHAR(300),
    motivo_rejeicao TEXT,
    prescritor_id   UUID        REFERENCES prescritores(id),
    cliente_id      UUID        REFERENCES clientes(id),
    farmaceutico_id UUID        REFERENCES farmaceuticos(id),  -- quem validou
    data_validacao  TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Itens da Receita (medicamentos prescritos) ────────────────
CREATE TABLE itens_receita (
    id             UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    receita_id     UUID    NOT NULL REFERENCES receitas(id) ON DELETE CASCADE,
    medicamento_id UUID    NOT NULL REFERENCES medicamentos(id),
    quantidade     INTEGER NOT NULL,
    posologia      TEXT,
    observacao     TEXT
);

-- ── Registros SNGPC ───────────────────────────────────────────
-- Cada dispensação de medicamento controlado gera um registro
CREATE TABLE registros_sngpc (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    receita_id          UUID        NOT NULL REFERENCES receitas(id),
    medicamento_id      UUID        NOT NULL REFERENCES medicamentos(id),
    lote_id             UUID        NOT NULL REFERENCES lotes(id),
    -- comprador
    comprador_nome      VARCHAR(150) NOT NULL,
    comprador_cpf       CHAR(11)    NOT NULL,
    comprador_rg        VARCHAR(20),
    -- quantidade dispensada
    quantidade          INTEGER     NOT NULL,
    -- envio ao governo
    status_envio        VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
                        CHECK (status_envio IN ('PENDENTE','ENVIADO',
                                               'CONFIRMADO','ERRO','REJEITADO')),
    data_registro       TIMESTAMP   NOT NULL DEFAULT NOW(),
    data_envio          TIMESTAMP,
    numero_protocolo    VARCHAR(50),  -- número retornado pelo SNGPC
    retorno_governo     TEXT,
    tentativas_envio    INTEGER     NOT NULL DEFAULT 0,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Índices ───────────────────────────────────────────────────
CREATE INDEX idx_receitas_status       ON receitas(status);
CREATE INDEX idx_receitas_tipo         ON receitas(tipo);
CREATE INDEX idx_receitas_cliente_id   ON receitas(cliente_id);
CREATE INDEX idx_sngpc_status_envio    ON registros_sngpc(status_envio);
CREATE INDEX idx_sngpc_data_registro   ON registros_sngpc(data_registro);
CREATE INDEX idx_prescritores_crm      ON prescritores(crm, uf_crm);
