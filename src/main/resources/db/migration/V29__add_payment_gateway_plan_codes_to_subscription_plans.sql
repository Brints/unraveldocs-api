-- Add payment gateway plan code columns to subscription_plans table
-- These columns store the external plan IDs from Paystack and Stripe

ALTER TABLE subscription_plans
ADD COLUMN IF NOT EXISTS paystack_plan_code VARCHAR(100);

ALTER TABLE subscription_plans
ADD COLUMN IF NOT EXISTS stripe_price_id VARCHAR(100);

-- Add indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_subscription_plans_paystack_plan_code
ON subscription_plans(paystack_plan_code);

CREATE INDEX IF NOT EXISTS idx_subscription_plans_stripe_price_id
ON subscription_plans(stripe_price_id);
