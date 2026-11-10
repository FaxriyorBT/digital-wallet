-- Technical wallets that act as the other side of ledger postings:
--   FUNDING  - money entering the system through top-ups (its ledger balance is negative)
--   FEE      - collected transfer fees
--   EXCHANGE - currency position used by cross-currency transfers
-- Their numbers fail the Luhn check on purpose, so they can never be used as a transfer target.
INSERT INTO wallets (id, user_id, number, currency, type, status, balance, created_at, updated_at, version)
VALUES (gen_random_uuid(), NULL, '0000000000000101', 'UZS', 'FUNDING', 'ACTIVE', NULL, now(), now(), 0),
       (gen_random_uuid(), NULL, '0000000000000102', 'USD', 'FUNDING', 'ACTIVE', NULL, now(), now(), 0),
       (gen_random_uuid(), NULL, '0000000000000103', 'EUR', 'FUNDING', 'ACTIVE', NULL, now(), now(), 0),
       (gen_random_uuid(), NULL, '0000000000000201', 'UZS', 'FEE', 'ACTIVE', NULL, now(), now(), 0),
       (gen_random_uuid(), NULL, '0000000000000202', 'USD', 'FEE', 'ACTIVE', NULL, now(), now(), 0),
       (gen_random_uuid(), NULL, '0000000000000203', 'EUR', 'FEE', 'ACTIVE', NULL, now(), now(), 0),
       (gen_random_uuid(), NULL, '0000000000000301', 'UZS', 'EXCHANGE', 'ACTIVE', NULL, now(), now(), 0),
       (gen_random_uuid(), NULL, '0000000000000302', 'USD', 'EXCHANGE', 'ACTIVE', NULL, now(), now(), 0),
       (gen_random_uuid(), NULL, '0000000000000303', 'EUR', 'EXCHANGE', 'ACTIVE', NULL, now(), now(), 0);
