-- Add storage allocation fields to subscription plans and tracking tables

-- Add storage_limit to subscription_plans (individual plans)
ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS storage_limit BIGINT NOT NULL DEFAULT 125829120; -- Default 120 MB

-- Add storage_limit to team_subscription_plans (team plans)
-- NULL means unlimited (Enterprise plan)
ALTER TABLE team_subscription_plans
    ADD COLUMN IF NOT EXISTS storage_limit BIGINT DEFAULT NULL;

-- Add storage_used to user_subscriptions (individual user tracking)
ALTER TABLE user_subscriptions
    ADD COLUMN IF NOT EXISTS storage_used BIGINT NOT NULL DEFAULT 0;

-- Add storage_used to teams (team-level tracking)
ALTER TABLE teams
    ADD COLUMN IF NOT EXISTS storage_used BIGINT NOT NULL DEFAULT 0;

-- Update existing individual plans with correct storage limits
-- Free: 120 MB (125829120 bytes)
UPDATE subscription_plans SET storage_limit = 125829120 WHERE name = 'FREE';

-- Starter: 2.6 GB (2791728742 bytes)
UPDATE subscription_plans SET storage_limit = 2791728742 WHERE name IN ('STARTER_MONTHLY', 'STARTER_YEARLY');

-- Pro: 12.3 GB (13207604838 bytes)
UPDATE subscription_plans SET storage_limit = 13207604838 WHERE name IN ('PRO_MONTHLY', 'PRO_YEARLY');

-- Business: 30 GB (32212254720 bytes)
UPDATE subscription_plans SET storage_limit = 32212254720 WHERE name IN ('BUSINESS_MONTHLY', 'BUSINESS_YEARLY');

-- Update existing team plans with correct storage limits
-- Team Premium: 200 GB (214748364800 bytes)
UPDATE team_subscription_plans SET storage_limit = 214748364800 WHERE name = 'TEAM_PREMIUM';

-- Team Enterprise: Unlimited (NULL)
UPDATE team_subscription_plans SET storage_limit = NULL WHERE name = 'TEAM_ENTERPRISE';
