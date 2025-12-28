-- V31: Create Team tables (refactored from organizations)
-- Teams are subscription-based workspaces for document collaboration

-- Teams table
CREATE TABLE IF NOT EXISTS teams (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    team_code VARCHAR(8) NOT NULL UNIQUE,
    
    -- Subscription
    subscription_type VARCHAR(50) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    subscription_status VARCHAR(30) NOT NULL DEFAULT 'TRIAL',
    
    -- Payment Gateway
    payment_gateway VARCHAR(50),
    payment_gateway_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    paystack_subscription_code VARCHAR(255),
    
    -- Trial Management
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    has_used_trial BOOLEAN NOT NULL DEFAULT FALSE,
    trial_reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Billing Dates
    next_billing_date TIMESTAMP WITH TIME ZONE,
    last_billing_date TIMESTAMP WITH TIME ZONE,
    
    -- Auto-renewal and Cancellation
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    cancellation_requested_at TIMESTAMP WITH TIME ZONE,
    subscription_ends_at TIMESTAMP WITH TIME ZONE,
    
    -- Pricing Record
    subscription_price DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Status Flags
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP WITH TIME ZONE,
    
    -- Owner Reference
    created_by_id VARCHAR(36) NOT NULL,
    
    -- Document Limits
    max_members INTEGER NOT NULL DEFAULT 10,
    monthly_document_limit INTEGER,
    monthly_document_upload_count INTEGER NOT NULL DEFAULT 0,
    document_count_reset_at TIMESTAMP WITH TIME ZONE,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_teams_created_by FOREIGN KEY (created_by_id) REFERENCES users(id)
);

-- Team Members table
CREATE TABLE IF NOT EXISTS team_members (
    id VARCHAR(36) PRIMARY KEY,
    team_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    invited_by_id VARCHAR(36),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_team_members_invited_by FOREIGN KEY (invited_by_id) REFERENCES users(id),
    CONSTRAINT uk_team_members_team_user UNIQUE (team_id, user_id)
);

-- Team Invitations table
CREATE TABLE IF NOT EXISTS team_invitations (
    id VARCHAR(36) PRIMARY KEY,
    team_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    invited_by_id VARCHAR(36),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_team_invitations_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);

-- Team OTP Verifications table
CREATE TABLE IF NOT EXISTS team_otp_verifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    team_name VARCHAR(255) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_team_otp_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Indexes for Teams
CREATE INDEX IF NOT EXISTS idx_teams_team_code ON teams(team_code);
CREATE INDEX IF NOT EXISTS idx_teams_created_by ON teams(created_by_id);
CREATE INDEX IF NOT EXISTS idx_teams_is_active ON teams(is_active);
CREATE INDEX IF NOT EXISTS idx_teams_is_closed ON teams(is_closed);
CREATE INDEX IF NOT EXISTS idx_teams_subscription_type ON teams(subscription_type);
CREATE INDEX IF NOT EXISTS idx_teams_subscription_status ON teams(subscription_status);
CREATE INDEX IF NOT EXISTS idx_teams_trial_ends_at ON teams(trial_ends_at);
CREATE INDEX IF NOT EXISTS idx_teams_next_billing_date ON teams(next_billing_date);

-- Indexes for Team Members
CREATE INDEX IF NOT EXISTS idx_team_members_team ON team_members(team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user ON team_members(user_id);
CREATE INDEX IF NOT EXISTS idx_team_members_role ON team_members(role);

-- Indexes for Team Invitations
CREATE INDEX IF NOT EXISTS idx_team_invitations_team ON team_invitations(team_id);
CREATE INDEX IF NOT EXISTS idx_team_invitations_token ON team_invitations(invitation_token);
CREATE INDEX IF NOT EXISTS idx_team_invitations_email ON team_invitations(email);
CREATE INDEX IF NOT EXISTS idx_team_invitations_status ON team_invitations(status);
CREATE INDEX IF NOT EXISTS idx_team_invitations_expires_at ON team_invitations(expires_at);

-- Indexes for Team OTP
CREATE INDEX IF NOT EXISTS idx_team_otp_user ON team_otp_verifications(user_id);
CREATE INDEX IF NOT EXISTS idx_team_otp_expires_at ON team_otp_verifications(expires_at);

-- View for team statistics
CREATE OR REPLACE VIEW team_stats AS
SELECT 
    t.id AS team_id,
    t.name AS team_name,
    t.team_code,
    t.subscription_type,
    t.billing_cycle,
    t.subscription_status,
    t.is_active,
    t.trial_ends_at,
    t.next_billing_date,
    t.subscription_price,
    t.currency,
    t.auto_renew,
    COUNT(tm.id) AS member_count,
    t.max_members,
    t.monthly_document_upload_count,
    t.monthly_document_limit,
    t.created_at
FROM teams t
LEFT JOIN team_members tm ON t.id = tm.team_id
GROUP BY t.id;

-- Function to reset monthly document counts (to be called by scheduler)
CREATE OR REPLACE FUNCTION reset_team_monthly_document_counts()
RETURNS void AS $$
BEGIN
    UPDATE teams 
    SET monthly_document_upload_count = 0,
        document_count_reset_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE is_active = true 
      AND (document_count_reset_at IS NULL 
           OR document_count_reset_at < DATE_TRUNC('month', CURRENT_TIMESTAMP));
END;
$$ LANGUAGE plpgsql;
