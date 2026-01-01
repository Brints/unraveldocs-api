-- Clean up old subscription plan names that don't match current SubscriptionPlans enum
-- Current valid values: FREE, STARTER_MONTHLY, STARTER_YEARLY, PRO_MONTHLY, PRO_YEARLY, BUSINESS_MONTHLY, BUSINESS_YEARLY

-- STEP 1: First, update user_subscriptions that reference deprecated plans to use FREE plan
-- This must happen BEFORE deleting the old plans to avoid foreign key violations
UPDATE user_subscriptions 
SET plan_id = (SELECT id FROM subscription_plans WHERE name = 'FREE' LIMIT 1),
    updated_at = CURRENT_TIMESTAMP
WHERE plan_id IN (
    SELECT id FROM subscription_plans 
    WHERE name NOT IN (
        'FREE', 
        'STARTER_MONTHLY', 
        'STARTER_YEARLY', 
        'PRO_MONTHLY', 
        'PRO_YEARLY', 
        'BUSINESS_MONTHLY', 
        'BUSINESS_YEARLY'
    )
);

-- STEP 2: Now safely delete the deprecated plans (no more references)
DELETE FROM subscription_plans 
WHERE name NOT IN (
    'FREE', 
    'STARTER_MONTHLY', 
    'STARTER_YEARLY', 
    'PRO_MONTHLY', 
    'PRO_YEARLY', 
    'BUSINESS_MONTHLY', 
    'BUSINESS_YEARLY'
);
