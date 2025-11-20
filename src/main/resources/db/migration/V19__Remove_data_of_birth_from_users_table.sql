-- Remove date_of_birth from users table
ALTER TABLE users DROP COLUMN date_of_birth;

-- Remove index on date_of_birth if it exists
DROP INDEX IF EXISTS idx_users_date_of_birth;
