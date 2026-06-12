-- ============================================================
-- V5 — PDV, Caixa, Vendas e Pagamentos
-- ============================================================

-- ── PDV (Ponto de Venda) ──────────────────────────────────────
CREATE TABLE pdvs (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    numero     VARCHAR(10) NOT NULL UNIQUE,
    descricao  VARCHAR(80),
    status     VARCHAR(15) NOT NULL DEFAULT 'FECHADO'
               CHECK (status IN ('ABERTO','FECHADO','BLOQUEADO','MANUTENCAO')),
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Caixas ────────────────────────────────────────────────────
CREATE TABLE caixas (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    pdv_id              UUID          NOT NULL REFERENCES pdvs(id),
    funcionario_id      UUID          NOT NULL REFERENCES funcionarios(id),
    abertura            TIMESTAMP     NOT NULL DEFAULT NOW(),
    fechamento          TIMESTAMP,
    saldo_abertura      NUMERIC(12,2) NOT NULL DEFAULT 0,
    saldo_fechamento    NUMERIC(12,2),
    total_vendas        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_entradas      NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_saidas        NUMERIC(12,2) NOT NULL DEFAULT 0,
    status              VARCHAR(15)   NOT NULL DEFAULT 'ABERTO'
                        CHECK (status IN ('ABERTO','FECHADO','CONFERIDO')),
    observacao          TEXT
);

-- ── Vendas ────────────────────────────────────────────────────
CREATE TABLE vendas (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    pdv_id              UUID          NOT NULL REFERENCES pdvs(id),
    caixa_id            UUID          NOT NULL REFERENCES caixas(id),
    funcionario_id      UUID          NOT NULL REFERENCES funcionarios(id),
    cliente_id          UUID          REFERENCES clientes(id),
    receita_id          UUID          REFERENCES receitas(id),
    data_hora           TIMESTAMP     NOT NULL DEFAULT NOW(),
    status              VARCHAR(20)   NOT NULL DEFAULT 'ABERTA'
                        CHECK (status IN ('ABERTA','FINALIZADA','CANCELADA','SUSPENSA')),
    tipo_atendimento    VARCHAR(20)   NOT NULL DEFAULT 'BALCAO'
                        CHECK (tipo_atendimento IN ('BALCAO','DELIVERY','TELEFONE')),
    subtotal            NUMERIC(12,2) NOT NULL DEFAULT 0,
    desconto            NUMERIC(12,2) NOT NULL DEFAULT 0,
    total               NUMERIC(12,2) NOT NULL DEFAULT 0,
    numero_cupom        VARCHAR(30),
    observacao          TEXT,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Itens da Venda ────────────────────────────────────────────
CREATE TABLE itens_venda (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    venda_id          UUID          NOT NULL REFERENCES vendas(id) ON DELETE CASCADE,
    medicamento_id    UUID          NOT NULL REFERENCES medicamentos(id),
    lote_id           UUID          NOT NULL REFERENCES lotes(id),
    quantidade        INTEGER       NOT NULL CHECK (quantidade > 0),
    preco_unitario    NUMERIC(10,2) NOT NULL,
    desconto          NUMERIC(10,2) NOT NULL DEFAULT 0,
    subtotal          NUMERIC(12,2) NOT NULL
);

-- ── Pagamentos ────────────────────────────────────────────────
CREATE TABLE pagamentos (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    venda_id     UUID          NOT NULL REFERENCES vendas(id),
    forma        VARCHAR(20)   NOT NULL
                 CHECK (forma IN ('DINHEIRO','CARTAO_DEBITO','CARTAO_CREDITO',
                                 'PIX','CONVENIO','VALE_FARMACIA','CREDIARIO')),
    valor        NUMERIC(12,2) NOT NULL,
    troco        NUMERIC(12,2) NOT NULL DEFAULT 0,
    nsu          VARCHAR(30),   -- número de sequência único (cartão/PIX)
    autorizacao  VARCHAR(30),
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE'
                 CHECK (status IN ('PENDENTE','APROVADO','RECUSADO','ESTORNADO')),
    data_hora    TIMESTAMP     NOT NULL DEFAULT NOW(),
    convenio_id  UUID          REFERENCES convenios(id)
);

-- ── Lançamentos de Caixa ─────────────────────────────────────
CREATE TABLE lancamentos_caixa (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    caixa_id        UUID          NOT NULL REFERENCES caixas(id),
    tipo            VARCHAR(15)   NOT NULL CHECK (tipo IN ('ENTRADA','SAIDA')),
    valor           NUMERIC(12,2) NOT NULL,
    descricao       VARCHAR(200),
    referencia_id   UUID,         -- UUID da venda ou pagamento
    data_hora       TIMESTAMP     NOT NULL DEFAULT NOW(),
    funcionario_id  UUID          NOT NULL REFERENCES funcionarios(id)
);

-- ── Índices ───────────────────────────────────────────────────
CREATE INDEX idx_vendas_data_hora       ON vendas(data_hora);
CREATE INDEX idx_vendas_cliente_id      ON vendas(cliente_id);
CREATE INDEX idx_vendas_status          ON vendas(status);
CREATE INDEX idx_itens_venda_venda_id   ON itens_venda(venda_id);
CREATE INDEX idx_itens_venda_lote_id    ON itens_venda(lote_id);
CREATE INDEX idx_pagamentos_venda_id    ON pagamentos(venda_id);
CREATE INDEX idx_caixas_pdv_status      ON caixas(pdv_id, status);

-- ── Inserção de dados iniciais ────────────────────────────────
INSERT INTO pdvs (numero, descricao, status) VALUES
    ('PDV-01', 'Caixa Principal',   'FECHADO'),
    ('PDV-02', 'Caixa Secundário',  'FECHADO');
