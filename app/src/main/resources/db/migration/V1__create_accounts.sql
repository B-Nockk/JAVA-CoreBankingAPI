-- V1: Create accounts table
-- Stores account aggregate root state.
-- Balance is NOT stored here — it is derived from the transactions ledger.

CREATE TABLE accounts (
    id             UUID            NOT NULL,
    account_number VARCHAR(10)     NOT NULL,
    owner_name     VARCHAR(255)    NOT NULL,
    currency       VARCHAR(3)      NOT NULL,
    status         VARCHAR(10)     NOT NULL,
    created_at     TIMESTAMPTZ     NOT NULL,
    created_by     VARCHAR(255)    NOT NULL,

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number)
);