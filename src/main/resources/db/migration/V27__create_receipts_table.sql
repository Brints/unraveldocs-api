-- Create receipts table for tracking payment receipts
CREATE TABLE receipts (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    receipt_number VARCHAR(50) NOT NULL UNIQUE,
    payment_provider VARCHAR(20) NOT NULL,
    external_payment_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(50),
    payment_method_details VARCHAR(100),
    description TEXT,
    receipt_url TEXT,
    paid_at TIMESTAMP WITH TIME ZONE,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_receipts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Create indexes for receipts table
CREATE INDEX idx_receipt_number ON receipts(receipt_number);
CREATE INDEX idx_receipt_user_id ON receipts(user_id);
CREATE INDEX idx_receipt_external_payment ON receipts(external_payment_id, payment_provider);
CREATE INDEX idx_receipt_created_at ON receipts(created_at);
