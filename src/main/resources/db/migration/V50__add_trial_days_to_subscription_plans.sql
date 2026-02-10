-- Add trial_days column to subscription_plans table
ALTER TABLE subscription_plans ADD COLUMN trial_days INTEGER NOT NULL DEFAULT 10;

-- Add comment explaining the column
COMMENT ON COLUMN subscription_plans.trial_days IS 'Number of trial days for new subscriptions (default 10 days)';
