-- Add sender_id column for tracking credit transfer provenance
ALTER TABLE credit_transactions ADD COLUMN sender_id VARCHAR(255);

ALTER TABLE credit_transactions ADD CONSTRAINT fk_credit_transaction_sender
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_credit_transactions_sender ON credit_transactions(sender_id);
