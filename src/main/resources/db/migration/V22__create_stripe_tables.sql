-- Create stripe_customers table
CREATE TABLE IF NOT EXISTS stripe_customers (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    stripe_customer_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255),
    default_payment_method_id VARCHAR(255),
    metadata TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stripe_customer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for stripe_customers
CREATE INDEX idx_stripe_customer_id ON stripe_customers(stripe_customer_id);
CREATE INDEX idx_user_id ON stripe_customers(user_id);

-- Create stripe_payments table
CREATE TABLE IF NOT EXISTS stripe_payments (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    stripe_customer_id VARCHAR(255),
    payment_intent_id VARCHAR(255),
    subscription_id VARCHAR(255),
    invoice_id VARCHAR(255),
    checkout_session_id VARCHAR(255),
    payment_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount_refunded DECIMAL(10, 2) DEFAULT 0.00,
    payment_method_id VARCHAR(255),
    receipt_url TEXT,
    description TEXT,
    failure_message TEXT,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stripe_payment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_stripe_payment_customer FOREIGN KEY (stripe_customer_id) REFERENCES stripe_customers(id) ON DELETE SET NULL
);

-- Create indexes for stripe_payments
CREATE INDEX idx_payment_intent_id ON stripe_payments(payment_intent_id);
CREATE INDEX idx_subscription_id ON stripe_payments(subscription_id);
CREATE INDEX idx_user_id_payment ON stripe_payments(user_id);
CREATE INDEX idx_status ON stripe_payments(status);
CREATE INDEX idx_created_at ON stripe_payments(created_at);

-- Create stripe_webhook_events table
CREATE TABLE IF NOT EXISTS stripe_webhook_events (
    id VARCHAR(255) PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing_error TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for stripe_webhook_events
CREATE UNIQUE INDEX idx_event_id ON stripe_webhook_events(event_id);
CREATE INDEX idx_processed ON stripe_webhook_events(processed);
CREATE INDEX idx_created_at_webhook ON stripe_webhook_events(created_at);
