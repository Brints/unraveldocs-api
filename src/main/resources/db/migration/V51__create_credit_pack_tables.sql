-- Credit Packs: available credit pack definitions
CREATE TABLE credit_packs (
    id VARCHAR(255) NOT NULL DEFAULT gen_random_uuid()::VARCHAR,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    price_in_cents BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    credits_included INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    paystack_product_code VARCHAR(255),
    stripe_price_id VARCHAR(255),
    paypal_product_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id)
);

-- User Credit Balances: tracks each user's credit balance
CREATE TABLE user_credit_balances (
    id VARCHAR(255) NOT NULL DEFAULT gen_random_uuid()::VARCHAR,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    balance INTEGER NOT NULL DEFAULT 0,
    total_purchased INTEGER NOT NULL DEFAULT 0,
    total_used INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_credit_balance_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Credit Transactions: audit log of all credit changes
CREATE TABLE credit_transactions (
    id VARCHAR(255) NOT NULL DEFAULT gen_random_uuid()::VARCHAR,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(30) NOT NULL,
    amount INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    description VARCHAR(500),
    reference_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_credit_transaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for efficient queries
CREATE INDEX idx_credit_transactions_user_id ON credit_transactions(user_id);
CREATE INDEX idx_credit_transactions_created_at ON credit_transactions(created_at DESC);
CREATE INDEX idx_credit_transactions_type ON credit_transactions(type);
CREATE INDEX idx_credit_packs_is_active ON credit_packs(is_active);

-- Seed default credit packs
INSERT INTO credit_packs (name, display_name, price_in_cents, currency, credits_included) VALUES
    ('STARTER_PACK', 'Starter Pack', 500, 'USD', 20),
    ('VALUE_PACK', 'Value Pack', 1500, 'USD', 75),
    ('POWER_PACK', 'Power Pack', 3000, 'USD', 200);
