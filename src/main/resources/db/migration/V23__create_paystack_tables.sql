-- Create Paystack-related tables for payment processing

-- Paystack Customers table
CREATE TABLE IF NOT EXISTS paystack_customers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    customer_code VARCHAR(100) UNIQUE,
    paystack_customer_id BIGINT,
    email VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    risk_action VARCHAR(50),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paystack_customer_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for paystack_customers
CREATE INDEX IF NOT EXISTS idx_paystack_customer_code ON paystack_customers(customer_code);
CREATE INDEX IF NOT EXISTS idx_paystack_customer_user_id ON paystack_customers(user_id);

-- Paystack Plans table
CREATE TABLE IF NOT EXISTS paystack_plans (
    id VARCHAR(36) PRIMARY KEY,
    paystack_plan_id BIGINT,
    plan_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    amount DECIMAL(10, 2) NOT NULL,
    interval VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    send_invoices BOOLEAN DEFAULT TRUE,
    send_sms BOOLEAN DEFAULT FALSE,
    invoice_limit INTEGER,
    hosted_page BOOLEAN DEFAULT FALSE,
    hosted_page_url TEXT,
    is_deleted BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    subscription_plan_id VARCHAR(36),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paystack_plan_subscription FOREIGN KEY (subscription_plan_id) REFERENCES subscription_plans(id) ON DELETE SET NULL
);

-- Create index for paystack_plans
CREATE INDEX IF NOT EXISTS idx_paystack_plan_code ON paystack_plans(plan_code);

-- Paystack Subscriptions table
CREATE TABLE IF NOT EXISTS paystack_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    paystack_customer_id VARCHAR(36),
    plan_code VARCHAR(100),
    paystack_plan_id VARCHAR(36),
    paystack_subscription_id BIGINT,
    subscription_code VARCHAR(100) NOT NULL UNIQUE,
    email_token VARCHAR(255),
    authorization_code VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2),
    cron_expression VARCHAR(100),
    next_payment_date TIMESTAMP WITH TIME ZONE,
    invoice_limit INTEGER,
    payments_count INTEGER DEFAULT 0,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paystack_subscription_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_paystack_subscription_customer FOREIGN KEY (paystack_customer_id) REFERENCES paystack_customers(id) ON DELETE SET NULL,
    CONSTRAINT fk_paystack_subscription_plan FOREIGN KEY (paystack_plan_id) REFERENCES paystack_plans(id) ON DELETE SET NULL
);

-- Create indexes for paystack_subscriptions
CREATE INDEX IF NOT EXISTS idx_paystack_subscription_code ON paystack_subscriptions(subscription_code);
CREATE INDEX IF NOT EXISTS idx_paystack_subscription_user_id ON paystack_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_paystack_subscription_status ON paystack_subscriptions(status);

-- Paystack Payments table
CREATE TABLE IF NOT EXISTS paystack_payments (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    paystack_customer_id VARCHAR(36),
    transaction_id BIGINT,
    reference VARCHAR(100) NOT NULL UNIQUE,
    access_code VARCHAR(255),
    authorization_url TEXT,
    authorization_code VARCHAR(255),
    subscription_code VARCHAR(100),
    plan_code VARCHAR(100),
    payment_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount_refunded DECIMAL(10, 2) DEFAULT 0,
    fees DECIMAL(10, 2),
    channel VARCHAR(50),
    gateway_response VARCHAR(500),
    ip_address VARCHAR(50),
    description TEXT,
    failure_message TEXT,
    metadata TEXT,
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_paystack_payment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_paystack_payment_customer FOREIGN KEY (paystack_customer_id) REFERENCES paystack_customers(id) ON DELETE SET NULL
);

-- Create indexes for paystack_payments
CREATE INDEX IF NOT EXISTS idx_paystack_reference ON paystack_payments(reference);
CREATE INDEX IF NOT EXISTS idx_paystack_transaction_id ON paystack_payments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_paystack_payment_user_id ON paystack_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_paystack_payment_status ON paystack_payments(status);
CREATE INDEX IF NOT EXISTS idx_paystack_payment_created_at ON paystack_payments(created_at);

