-- Create PayPal-related tables for payment processing

-- PayPal Customers table
CREATE TABLE IF NOT EXISTS paypal_customers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    payer_id VARCHAR(100) UNIQUE,
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    country_code VARCHAR(5),
    vault_id VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paypal_customer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for paypal_customers
CREATE INDEX IF NOT EXISTS idx_paypal_customer_payer_id ON paypal_customers(payer_id);
CREATE INDEX IF NOT EXISTS idx_paypal_customer_user_id ON paypal_customers(user_id);

-- PayPal Payments table
CREATE TABLE IF NOT EXISTS paypal_payments (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    paypal_customer_id VARCHAR(36),
    order_id VARCHAR(100) UNIQUE,
    capture_id VARCHAR(100),
    authorization_id VARCHAR(100),
    subscription_id VARCHAR(100),
    payment_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount_refunded DECIMAL(10, 2) DEFAULT 0,
    paypal_fee DECIMAL(10, 2),
    net_amount DECIMAL(10, 2),
    intent VARCHAR(20),
    payer_id VARCHAR(100),
    payer_email VARCHAR(255),
    description TEXT,
    failure_message TEXT,
    metadata TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paypal_payment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_paypal_payment_customer FOREIGN KEY (paypal_customer_id) REFERENCES paypal_customers(id) ON DELETE SET NULL
);

-- Create indexes for paypal_payments
CREATE INDEX IF NOT EXISTS idx_paypal_order_id ON paypal_payments(order_id);
CREATE INDEX IF NOT EXISTS idx_paypal_capture_id ON paypal_payments(capture_id);
CREATE INDEX IF NOT EXISTS idx_paypal_payment_user_id ON paypal_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_paypal_payment_status ON paypal_payments(status);
CREATE INDEX IF NOT EXISTS idx_paypal_payment_created_at ON paypal_payments(created_at);

-- PayPal Subscriptions table
CREATE TABLE IF NOT EXISTS paypal_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    paypal_customer_id VARCHAR(36),
    subscription_id VARCHAR(100) NOT NULL UNIQUE,
    plan_id VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2),
    currency VARCHAR(3),
    custom_id VARCHAR(255),
    start_time TIMESTAMP WITH TIME ZONE,
    next_billing_time TIMESTAMP WITH TIME ZONE,
    outstanding_balance DECIMAL(10, 2),
    cycles_completed INTEGER,
    failed_payments_count INTEGER DEFAULT 0,
    last_payment_time TIMESTAMP WITH TIME ZONE,
    last_payment_amount DECIMAL(10, 2),
    auto_renewal BOOLEAN DEFAULT TRUE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    status_change_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paypal_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_paypal_subscription_customer FOREIGN KEY (paypal_customer_id) REFERENCES paypal_customers(id) ON DELETE SET NULL
);

-- Create indexes for paypal_subscriptions
CREATE INDEX IF NOT EXISTS idx_paypal_subscription_id ON paypal_subscriptions(subscription_id);
CREATE INDEX IF NOT EXISTS idx_paypal_subscription_user_id ON paypal_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_paypal_subscription_status ON paypal_subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_paypal_subscription_plan_id ON paypal_subscriptions(plan_id);

-- PayPal Webhook Events table (for idempotency)
CREATE TABLE IF NOT EXISTS paypal_webhook_events (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    payload TEXT,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP WITH TIME ZONE,
    processing_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for paypal_webhook_events
CREATE INDEX IF NOT EXISTS idx_paypal_webhook_event_id ON paypal_webhook_events(event_id);
CREATE INDEX IF NOT EXISTS idx_paypal_webhook_event_type ON paypal_webhook_events(event_type);
CREATE INDEX IF NOT EXISTS idx_paypal_webhook_processed ON paypal_webhook_events(processed);

-- Add PayPal plan code to subscription_plans table
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS paypal_plan_code VARCHAR(100);
