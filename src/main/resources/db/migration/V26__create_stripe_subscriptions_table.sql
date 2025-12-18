-- V26: Create stripe_subscriptions table for local subscription tracking

CREATE TABLE stripe_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    stripe_customer_id VARCHAR(36) NOT NULL,
    stripe_subscription_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    price_id VARCHAR(255),
    product_id VARCHAR(255),
    plan_name VARCHAR(255),
    quantity BIGINT DEFAULT 1,
    currency VARCHAR(3) DEFAULT 'usd',
    current_period_start TIMESTAMP WITH TIME ZONE,
    current_period_end TIMESTAMP WITH TIME ZONE,
    trial_start TIMESTAMP WITH TIME ZONE,
    trial_end TIMESTAMP WITH TIME ZONE,
    cancel_at TIMESTAMP WITH TIME ZONE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    default_payment_method_id VARCHAR(255),
    latest_invoice_id VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_stripe_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_stripe_subscription_customer FOREIGN KEY (stripe_customer_id) REFERENCES stripe_customers(id) ON DELETE CASCADE
);

-- Indexes for common queries
CREATE INDEX idx_stripe_sub_user_id ON stripe_subscriptions(user_id);
CREATE INDEX idx_stripe_sub_stripe_id ON stripe_subscriptions(stripe_subscription_id);
CREATE INDEX idx_stripe_sub_status ON stripe_subscriptions(status);
CREATE INDEX idx_stripe_sub_customer ON stripe_subscriptions(stripe_customer_id);
CREATE INDEX idx_stripe_sub_current_period_end ON stripe_subscriptions(current_period_end);
