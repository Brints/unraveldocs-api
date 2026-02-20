-- Add AI operations tracking fields to subscription tables
ALTER TABLE subscription_plans ADD COLUMN ai_operations_limit INTEGER NOT NULL DEFAULT 0;
ALTER TABLE user_subscriptions ADD COLUMN ai_operations_used INTEGER NOT NULL DEFAULT 0;

-- Set default AI operation limits per tier
UPDATE subscription_plans SET ai_operations_limit = 5 WHERE name = 'Free';
UPDATE subscription_plans SET ai_operations_limit = 50 WHERE name IN ('Starter_Monthly', 'Starter_Yearly');
UPDATE subscription_plans SET ai_operations_limit = 200 WHERE name IN ('Pro_Monthly', 'Pro_Yearly');
UPDATE subscription_plans SET ai_operations_limit = 500 WHERE name IN ('Business_Monthly', 'Business_Yearly');
