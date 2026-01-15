-- V39: Add collection name and encryption fields
-- This migration adds:
-- 1. name column to document_collections for custom collection naming
-- 2. encryption fields to document_file_entries for AES-256 encryption support
-- 3. display_name to document_file_entries for user-customizable file names

-- Add name field to document_collections
ALTER TABLE document_collections ADD COLUMN name VARCHAR(255);

-- Backfill existing collections with auto-generated names based on ID prefix
UPDATE document_collections SET name = CONCAT('Collection-', SUBSTRING(id, 1, 8));

-- Make name NOT NULL after backfill
ALTER TABLE document_collections ALTER COLUMN name SET NOT NULL;

-- Add encryption fields to document_file_entries
ALTER TABLE document_file_entries ADD COLUMN is_encrypted BOOLEAN DEFAULT FALSE;
ALTER TABLE document_file_entries ADD COLUMN encryption_iv VARCHAR(64);

-- Add display name for individual files (user-customizable)
ALTER TABLE document_file_entries ADD COLUMN display_name VARCHAR(255);

-- Create indexes for efficient lookups
CREATE INDEX idx_doc_collections_name ON document_collections(name);
CREATE INDEX idx_doc_collections_user_name ON document_collections(user_id, name);
