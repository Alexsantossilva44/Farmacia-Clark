-- ============================================================
-- V2 — Estoque, Lotes e Movimentações
-- ============================================================

-- ── Item de Estoque (saldo consolidado por medicamento) ──────
CREATE TABLE itens_estoque (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    medicamento_id        UUID        NOT NULL UNIQUE REFERENCES medicamentos(id),
    quantidade_atual      INTEGER     NOT NULL DEFAULT 0 CHECK (quantidade_atual >= 0),
    quantidade_minima     INTEGER     NOT NULL DEFAULT 5,
    quantidade_maxima     INTEGER     NOT NULL DEFAULT 500,
    ultima_movimentacao   TIMESTAMP,
    created_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Fornecedores ─────────────────────────────────────────────
CREATE TABLE fornecedores (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    razao_social      VARCHAR(150) NOT NULL,
    nome_fantasia     VARCHAR(150),
    cnpj              CHAR(14)    NOT NULL UNIQUE,
    inscricao_estadual VARCHAR(20),
    email             VARCHAR(120),
    telefone          VARCHAR(20),
    -- endereço embutido (Value Object)
    logradouro        VARCHAR(200),
    numero            VARCHAR(10),
    complemento       VARCHAR(50),
    bairro            VARCHAR(80),
    cidade            VARCHAR(80),
    uf                CHAR(2),
    cep               CHAR(8),
    ativo             BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Pedidos de Compra ─────────────────────────────────────────
CREATE TABLE pedidos_compra (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    fornecedor_id         UUID        NOT NULL REFERENCES fornecedores(id),
    data_pedido           DATE        NOT NULL DEFAULT CURRENT_DATE,
    data_entrega_prevista DATE,
    status                VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO'
                          CHECK (status IN ('RASCUNHO','ENVIADO','CONFIRMADO',
                                           'PARCIALMENTE_RECEBIDO','RECEBIDO','CANCELADO')),
    valor_total           NUMERIC(12,2),
    observacao            TEXT,
    created_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE itens_pedido_compra (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_compra_id        UUID        NOT NULL REFERENCES pedidos_compra(id) ON DELETE CASCADE,
    medicamento_id          UUID        NOT NULL REFERENCES medicamentos(id),
    quantidade_solicitada   INTEGER     NOT NULL,
    quantidade_recebida     INTEGER     NOT NULL DEFAULT 0,
    preco_unitario          NUMERIC(10,2),
    UNIQUE (pedido_compra_id, medicamento_id)
);

-- ── Notas Fiscais de Entrada ──────────────────────────────────
CREATE TABLE notas_fiscais_entrada (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_compra_id  UUID        REFERENCES pedidos_compra(id),
    fornecedor_id     UUID        NOT NULL REFERENCES fornecedores(id),
    numero_nota       VARCHAR(20) NOT NULL,
    serie             VARCHAR(5),
    chave_acesso      CHAR(44)    UNIQUE,
    data_emissao      DATE        NOT NULL,
    data_entrada      DATE        NOT NULL DEFAULT CURRENT_DATE,
    valor_total       NUMERIC(12,2),
    status            VARCHAR(20) NOT NULL DEFAULT 'RECEBIDA'
                      CHECK (status IN ('RECEBIDA','CONFERIDA','DIVERGENCIA','CANCELADA')),
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Lotes ─────────────────────────────────────────────────────
CREATE TABLE lotes (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    medicamento_id        UUID        NOT NULL REFERENCES medicamentos(id),
    nota_fiscal_id        UUID        REFERENCES notas_fiscais_entrada(id),
    numero_lote           VARCHAR(30) NOT NULL,
    data_fabricacao       DATE,
    data_validade         DATE        NOT NULL,
    quantidade_recebida   INTEGER     NOT NULL CHECK (quantidade_recebida > 0),
    quantidade_atual      INTEGER     NOT NULL CHECK (quantidade_atual >= 0),
    preco_custo           NUMERIC(10,2),
    status                VARCHAR(20) NOT NULL DEFAULT 'ATIVO'
                          CHECK (status IN ('ATIVO','VENCIDO','BLOQUEADO','ESGOTADO','DEVOLVIDO')),
    created_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (medicamento_id, numero_lote)
);

-- ── Movimentações de Estoque ──────────────────────────────────
CREATE TABLE movimentacoes_estoque (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lote_id          UUID        NOT NULL REFERENCES lotes(id),
    medicamento_id   UUID        NOT NULL REFERENCES medicamentos(id),
    tipo             VARCHAR(30) NOT NULL
                     CHECK (tipo IN ('ENTRADA_COMPRA','ENTRADA_DEVOLUCAO_CLIENTE',
                                    'SAIDA_VENDA','SAIDA_VENCIMENTO','SAIDA_PERDA',
                                    'AJUSTE_POSITIVO','AJUSTE_NEGATIVO','TRANSFERENCIA')),
    quantidade       INTEGER     NOT NULL,
    saldo_anterior   INTEGER     NOT NULL,
    saldo_posterior  INTEGER     NOT NULL,
    referencia_id    UUID,        -- UUID da Venda ou NF que originou
    motivo_ajuste    TEXT,
    funcionario_id   UUID,        -- quem fez o ajuste (FK adicionada na V3)
    data_hora        TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Alertas de Estoque ────────────────────────────────────────
CREATE TABLE alertas_estoque (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    medicamento_id   UUID        NOT NULL REFERENCES medicamentos(id),
    lote_id          UUID        REFERENCES lotes(id),
    tipo             VARCHAR(30) NOT NULL
                     CHECK (tipo IN ('ESTOQUE_MINIMO','VENCIMENTO_PROXIMO',
                                    'LOTE_VENCIDO','ESTOQUE_ZERADO')),
    mensagem         TEXT        NOT NULL,
    data_geracao     TIMESTAMP   NOT NULL DEFAULT NOW(),
    lido             BOOLEAN     NOT NULL DEFAULT FALSE,
    status           VARCHAR(20) NOT NULL DEFAULT 'ABERTO'
                     CHECK (status IN ('ABERTO','EM_TRATAMENTO','RESOLVIDO','IGNORADO'))
);

-- ── Índices ───────────────────────────────────────────────────
CREATE INDEX idx_lotes_medicamento_id    ON lotes(medicamento_id);
CREATE INDEX idx_lotes_data_validade     ON lotes(data_validade);
CREATE INDEX idx_lotes_status            ON lotes(status);
CREATE INDEX idx_movimentacoes_lote_id   ON movimentacoes_estoque(lote_id);
CREATE INDEX idx_movimentacoes_data_hora ON movimentacoes_estoque(data_hora);
CREATE INDEX idx_alertas_lido            ON alertas_estoque(lido) WHERE lido = FALSE;
