-- Modelo do MVP: Statement -> Transaction.
--
-- Soft-delete: a coluna "deleted" nunca e removida fisicamente; o historico
-- precisa sobreviver para auditoria contabil.

CREATE TABLE statements (
    id                UUID         PRIMARY KEY,
    -- Identifica qual BankStatementParser processou o arquivo (ex.: "stone").
    bank_key          VARCHAR(40)  NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    -- Chave do objeto no storage (MinIO).
    storage_key       VARCHAR(512),
    -- Metadados lidos do cabecalho do PDF; podem faltar se o layout mudar.
    issued_at         DATE,
    document          VARCHAR(32),
    transaction_count INTEGER      NOT NULL DEFAULT 0,
    uploaded_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Versao do parser que gerou as transacoes, para auditoria.
    parser_version    VARCHAR(10),
    -- Resumo da checagem de continuidade de saldo; NULL quando nao ha divergencia.
    validation_flags  TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_statements_uploaded ON statements (uploaded_at DESC);

CREATE TABLE transactions (
    id               UUID           PRIMARY KEY,
    statement_id     UUID           NOT NULL REFERENCES statements (id),
    transaction_date DATE           NOT NULL,
    -- "Entrada" ou "Saida", como extraido do extrato.
    type             VARCHAR(20)    NOT NULL,
    -- Positivo para Entrada, negativo para Saida.
    amount           NUMERIC(15, 2) NOT NULL,
    balance          NUMERIC(15, 2),
    description      TEXT,
    detail           TEXT,
    -- Ordem original das linhas no PDF: o extrato nao e ordenavel so por data
    -- (varias transacoes no mesmo dia) e a sequencia importa para conferencia.
    line_number      INTEGER        NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_transactions_statement ON transactions (statement_id, line_number);
