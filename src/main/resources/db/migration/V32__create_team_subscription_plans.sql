-- V32: Create team subscription plans table for database-driven pricing
-- This allows flexible pricing management without code changes

CREATE TABLE IF NOT EXISTS team_subscription_plans (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Pricing
    monthly_price DECIMAL(10, 2) NOT NULL,
    yearly_price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Limits
    max_members INTEGER NOT NULL,
    monthly_document_limit INTEGER, -- NULL = unlimited
    
    -- Features
    has_admin_promotion BOOLEAN NOT NULL DEFAULT FALSE,
    has_email_invitations BOOLEAN NOT NULL DEFAULT FALSE,
    trial_days INTEGER NOT NULL DEFAULT 10,
    
    -- Payment Gateway Integration
    stripe_price_id_monthly VARCHAR(255),
    stripe_price_id_yearly VARCHAR(255),
    paystack_plan_code_monthly VARCHAR(255),
    paystack_plan_code_yearly VARCHAR(255),
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_team_subscription_plans_name ON team_subscription_plans(name);
CREATE INDEX IF NOT EXISTS idx_team_subscription_plans_active ON team_subscription_plans(is_active);

-- Seed default plans
INSERT INTO team_subscription_plans (
    id, name, display_name, description,
    monthly_price, yearly_price, currency,
    max_members, monthly_document_limit,
    has_admin_promotion, has_email_invitations, trial_days,
    is_active
) VALUES
(
    gen_random_uuid()::text,
    'TEAM_PREMIUM',
    'Team Premium',
    'Perfect for small teams. Includes 200 documents per month with up to 10 members.',
    29.00, 290.00, 'USD',
    10, 200,
    false, false, 10,
    true
),
(
    gen_random_uuid()::text,
    'TEAM_ENTERPRISE',
    'Team Enterprise',
    'For larger teams that need unlimited documents, admin roles, and email invitations.',
    79.00, 790.00, 'USD',
    15, NULL,
    true, true, 10,
    true
);

-- Update teams table to reference team_subscription_plans
-- Note: subscription_type column remains for backwards compatibility
-- but we add a foreign key to team_subscription_plans for price lookups
ALTER TABLE teams ADD COLUMN IF NOT EXISTS plan_id VARCHAR(36);

-- Create foreign key (optional, for referential integrity)
-- We don't make it NOT NULL to allow migration of existing teams
ALTER TABLE teams ADD CONSTRAINT fk_teams_plan 
    FOREIGN KEY (plan_id) REFERENCES team_subscription_plans(id);

CREATE INDEX IF NOT EXISTS idx_teams_plan_id ON teams(plan_id);
