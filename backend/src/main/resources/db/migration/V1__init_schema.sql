-- Modelo multi-tenant: Organization -> User / Client -> Statement -> Transaction
--
-- organization_id e repetido em statements e transactions (em vez de so
-- alcancavel via join com clients) para que o filtro de tenant obrigatorio
-- seja sempre um predicado indexado direto, sem join em cadeia.
--
-- Soft-delete: a coluna "deleted" nunca e removida fisicamente; o historico
-- precisa sobreviver para auditoria contabil.

CREATE TABLE organizations (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(160) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE users (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    name            VARCHAR(160) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_users_organization ON users (organization_id);
CREATE UNIQUE INDEX uq_users_org_email ON users (organization_id, email) WHERE deleted = FALSE;

CREATE TABLE clients (
    id              UUID         PRIMARY KEY,
    organization_id UUID         NOT NULL REFERENCES organizations (id),
    name            VARCHAR(255) NOT NULL,
    -- Apenas digitos (CNPJ ou CPF), sem mascara, para busca estavel.
    document        VARCHAR(14)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_clients_organization ON clients (organization_id);
-- Indice parcial: permite recadastrar um CNPJ cujo cliente anterior foi soft-deleted.
CREATE UNIQUE INDEX uq_clients_org_document ON clients (organization_id, document) WHERE deleted = FALSE;

CREATE TABLE statements (
    id                UUID         PRIMARY KEY,
    organization_id   UUID         NOT NULL REFERENCES organizations (id),
    client_id         UUID         NOT NULL REFERENCES clients (id),
    -- Identifica qual BankStatementParser processou o arquivo (ex.: "stone").
    bank_key          VARCHAR(40)  NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    -- Chave do objeto no storage (MinIO). Preenchida na Fase 3.
    storage_key       VARCHAR(512),
    -- Metadados lidos do cabecalho do PDF; podem faltar se o layout mudar.
    issued_at         DATE,
    document          VARCHAR(32),
    transaction_count INTEGER      NOT NULL DEFAULT 0,
    uploaded_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_statements_organization ON statements (organization_id);
CREATE INDEX idx_statements_client ON statements (client_id);
CREATE INDEX idx_statements_org_uploaded ON statements (organization_id, uploaded_at DESC);

CREATE TABLE transactions (
    id               UUID           PRIMARY KEY,
    organization_id  UUID           NOT NULL REFERENCES organizations (id),
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

CREATE INDEX idx_transactions_organization ON transactions (organization_id);
CREATE INDEX idx_transactions_statement ON transactions (statement_id, line_number);
CREATE INDEX idx_transactions_org_date ON transactions (organization_id, transaction_date DESC);
