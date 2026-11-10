CREATE TABLE exchange_rates
(
    id         UUID PRIMARY KEY,
    currency   VARCHAR(3)     NOT NULL,
    rate       NUMERIC(19, 4) NOT NULL,
    rate_date  DATE           NOT NULL,
    fetched_at TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_exchange_rates_currency_date UNIQUE (currency, rate_date),
    CONSTRAINT ck_exchange_rates_rate CHECK (rate > 0)
);
