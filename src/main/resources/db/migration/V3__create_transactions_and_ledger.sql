CREATE TABLE transactions
(
    id               UUID PRIMARY KEY,
    type             VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    initiator_id     UUID           NOT NULL REFERENCES users (id),
    idempotency_key  VARCHAR(64)    NOT NULL,
    source_wallet_id UUID REFERENCES wallets (id),
    target_wallet_id UUID           NOT NULL REFERENCES wallets (id),
    amount           NUMERIC(19, 2) NOT NULL,
    currency         VARCHAR(3)     NOT NULL,
    fee              NUMERIC(19, 2) NOT NULL,
    credited_amount  NUMERIC(19, 2),
    exchange_rate    NUMERIC(20, 10),
    decline_reason   VARCHAR(50),
    description      VARCHAR(140),
    created_at       TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (initiator_id, idempotency_key),
    CONSTRAINT ck_transactions_amounts CHECK (amount > 0 AND fee >= 0)
);

CREATE INDEX idx_transactions_source_wallet ON transactions (source_wallet_id, created_at);
CREATE INDEX idx_transactions_target_wallet ON transactions (target_wallet_id, created_at);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);

CREATE TABLE ledger_entries
(
    id             UUID PRIMARY KEY,
    transaction_id UUID           NOT NULL REFERENCES transactions (id),
    wallet_id      UUID           NOT NULL REFERENCES wallets (id),
    direction      VARCHAR(6)     NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL,
    balance_after  NUMERIC(19, 2),
    created_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_ledger_entries_amount CHECK (amount > 0)
);

CREATE INDEX idx_ledger_entries_wallet ON ledger_entries (wallet_id, created_at);
CREATE INDEX idx_ledger_entries_transaction ON ledger_entries (transaction_id);
