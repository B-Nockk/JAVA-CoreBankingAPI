-- V3: Create transfers table
-- Records the full lifecycle of a fund transfer between two accounts.
-- Only status and failure_reason are mutable after creation.

CREATE TABLE transfers (
    id                          UUID            NOT NULL,
    source_account_number       VARCHAR(10)     NOT NULL,
    destination_account_number  VARCHAR(10)     NOT NULL,
    amount                      DECIMAL(19, 4)  NOT NULL,
    currency                    VARCHAR(3)      NOT NULL,
    status                      VARCHAR(20)     NOT NULL,
    failure_reason              VARCHAR(255),
    created_at                  TIMESTAMPTZ     NOT NULL,
    created_by                  VARCHAR(255)    NOT NULL,

    CONSTRAINT pk_transfers PRIMARY KEY (id)
);

CREATE INDEX idx_transfers_source_account      ON transfers (source_account_number);
CREATE INDEX idx_transfers_destination_account ON transfers (destination_account_number);
CREATE INDEX idx_transfers_status              ON transfers (status);