ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_platform_admin BOOLEAN DEFAULT FALSE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_organization_admin BOOLEAN DEFAULT FALSE;

-- Update existing users to set is_platform_admin to FALSE
UPDATE users
SET is_platform_admin = FALSE
WHERE is_platform_admin IS NULL;
UPDATE users
SET is_organization_admin = FALSE
WHERE is_organization_admin IS NULL;

-- Create index for is_platform_admin
CREATE INDEX IF NOT EXISTS idx_users_is_platform_admin ON users(is_platform_admin);
CREATE INDEX IF NOT EXISTS idx_users_is_organization_admin ON users(is_organization_admin);