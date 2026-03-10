-- V2: Create transactions table
-- Append-only ledger — no UPDATE ever occurs on any business column.
-- balance_after on the last row IS the current account balance.

CREATE TABLE transactions (
    id           UUID            NOT NULL,
    account_id   UUID            NOT NULL,
    type         VARCHAR(20)     NOT NULL,
    amount       DECIMAL(19, 4)  NOT NULL,
    balance_after DECIMAL(19, 4) NOT NULL,
    currency     VARCHAR(3)      NOT NULL,
    reference    VARCHAR(255)    NOT NULL,
    created_at   TIMESTAMPTZ     NOT NULL,
    created_by   VARCHAR(255)    NOT NULL,

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id)
        REFERENCES accounts (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);