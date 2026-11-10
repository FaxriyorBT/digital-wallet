CREATE TABLE wallets
(
    id         UUID PRIMARY KEY,
    user_id    UUID REFERENCES users (id),
    number     VARCHAR(16)    NOT NULL,
    currency   VARCHAR(3)     NOT NULL,
    type       VARCHAR(20)    NOT NULL,
    status     VARCHAR(20)    NOT NULL,
    balance    NUMERIC(19, 2),
    created_at TIMESTAMPTZ    NOT NULL,
    updated_at TIMESTAMPTZ    NOT NULL,
    version    BIGINT         NOT NULL,
    CONSTRAINT uk_wallets_number UNIQUE (number),
    -- personal wallets belong to a user and can never go below zero;
    -- system wallets have no stored balance, it is calculated from the ledger
    CONSTRAINT ck_wallets_balance CHECK (
        (type = 'PERSONAL' AND user_id IS NOT NULL AND balance IS NOT NULL AND balance >= 0)
            OR (type <> 'PERSONAL' AND user_id IS NULL AND balance IS NULL)
        )
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);

-- a user can hold only one open wallet per currency
CREATE UNIQUE INDEX uk_wallets_user_currency ON wallets (user_id, currency) WHERE status <> 'CLOSED';
