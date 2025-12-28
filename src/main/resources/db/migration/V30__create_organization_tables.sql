-- Flyway migration: V30__create_organization_tables.sql
-- Creates tables for the organization feature

-- Organizations table
CREATE TABLE organizations (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    org_code VARCHAR(50) NOT NULL UNIQUE,
    subscription_type VARCHAR(20) NOT NULL,
    payment_gateway_customer_id VARCHAR(255),
    payment_gateway_subscription_id VARCHAR(255),
    payment_gateway VARCHAR(20),
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    has_used_trial BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_by_id VARCHAR(36) NOT NULL,
    max_members INTEGER NOT NULL DEFAULT 10,
    monthly_document_limit INTEGER,
    monthly_document_upload_count INTEGER NOT NULL DEFAULT 0,
    document_count_reset_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_organizations_created_by FOREIGN KEY (created_by_id) REFERENCES users(id)
);

-- Indexes for organizations
CREATE INDEX idx_organizations_org_code ON organizations(org_code);
CREATE INDEX idx_organizations_created_by ON organizations(created_by_id);
CREATE INDEX idx_organizations_is_active ON organizations(is_active);
CREATE INDEX idx_organizations_is_closed ON organizations(is_closed);
CREATE INDEX idx_organizations_subscription_type ON organizations(subscription_type);

-- Organization members table
CREATE TABLE organization_members (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    invited_by_id VARCHAR(36),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_org_members_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_org_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_org_members_invited_by FOREIGN KEY (invited_by_id) REFERENCES users(id),
    CONSTRAINT uk_org_members_org_user UNIQUE (organization_id, user_id)
);

-- Indexes for organization members
CREATE INDEX idx_org_members_organization ON organization_members(organization_id);
CREATE INDEX idx_org_members_user ON organization_members(user_id);
CREATE INDEX idx_org_members_role ON organization_members(role);

-- Organization invitations table
CREATE TABLE organization_invitations (
    id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    invited_by_id VARCHAR(36),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_org_invitations_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- Indexes for organization invitations
CREATE INDEX idx_org_invitations_organization ON organization_invitations(organization_id);
CREATE INDEX idx_org_invitations_token ON organization_invitations(invitation_token);
CREATE INDEX idx_org_invitations_email ON organization_invitations(email);
CREATE INDEX idx_org_invitations_status ON organization_invitations(status);
CREATE INDEX idx_org_invitations_expires_at ON organization_invitations(expires_at);

-- Organization OTP verifications table
CREATE TABLE organization_otp_verifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    organization_name VARCHAR(255) NOT NULL,
    organization_description TEXT,
    subscription_type VARCHAR(20) NOT NULL,
    payment_gateway VARCHAR(20),
    payment_token TEXT,
    otp VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for organization OTP verifications
CREATE INDEX idx_org_otp_user ON organization_otp_verifications(user_id);
CREATE INDEX idx_org_otp_otp ON organization_otp_verifications(otp);
CREATE INDEX idx_org_otp_expires ON organization_otp_verifications(expires_at);

-- Create a database view for organization statistics
CREATE OR REPLACE VIEW organization_stats AS
SELECT 
    o.id AS organization_id,
    o.name AS organization_name,
    o.subscription_type,
    o.is_active,
    o.is_closed,
    COUNT(DISTINCT om.id) AS member_count,
    COUNT(DISTINCT CASE WHEN om.role = 'ADMIN' THEN om.id END) AS admin_count,
    COUNT(DISTINCT oi.id) FILTER (WHERE oi.status = 'PENDING') AS pending_invitations,
    o.monthly_document_upload_count,
    o.monthly_document_limit,
    o.created_at
FROM organizations o
LEFT JOIN organization_members om ON o.id = om.organization_id
LEFT JOIN organization_invitations oi ON o.id = oi.organization_id
GROUP BY o.id, o.name, o.subscription_type, o.is_active, o.is_closed,
         o.monthly_document_upload_count, o.monthly_document_limit, o.created_at;

-- Function to reset monthly document counts (called by scheduled job)
CREATE OR REPLACE FUNCTION reset_org_monthly_document_counts()
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE organizations
    SET monthly_document_upload_count = 0,
        document_count_reset_at = CURRENT_TIMESTAMP
    WHERE monthly_document_upload_count > 0
      AND (document_count_reset_at IS NULL 
           OR document_count_reset_at < CURRENT_TIMESTAMP - INTERVAL '30 days');
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;
