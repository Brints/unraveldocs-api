-- V33: Add missing columns to team_otp_verifications table
-- These columns store the team creation request data during OTP verification flow

ALTER TABLE team_otp_verifications
ADD COLUMN IF NOT EXISTS team_description TEXT;

ALTER TABLE team_otp_verifications
ADD COLUMN IF NOT EXISTS subscription_type VARCHAR(50);

ALTER TABLE team_otp_verifications
ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(20);

ALTER TABLE team_otp_verifications
ADD COLUMN IF NOT EXISTS payment_gateway VARCHAR(50);

ALTER TABLE team_otp_verifications
ADD COLUMN IF NOT EXISTS payment_token VARCHAR(255);

-- Update existing rows to have default values for NOT NULL constraints
UPDATE team_otp_verifications 
SET subscription_type = 'TEAM_PREMIUM', billing_cycle = 'MONTHLY'
WHERE subscription_type IS NULL OR billing_cycle IS NULL;

-- Now make subscription_type and billing_cycle NOT NULL
ALTER TABLE team_otp_verifications
ALTER COLUMN subscription_type SET NOT NULL;

ALTER TABLE team_otp_verifications
ALTER COLUMN billing_cycle SET NOT NULL;
