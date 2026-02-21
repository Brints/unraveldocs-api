-- Add rich text editing support columns to ocr_data table
ALTER TABLE ocr_data ADD COLUMN IF NOT EXISTS edited_content TEXT;
ALTER TABLE ocr_data ADD COLUMN IF NOT EXISTS content_format VARCHAR(20);
ALTER TABLE ocr_data ADD COLUMN IF NOT EXISTS edited_by VARCHAR(255);
ALTER TABLE ocr_data ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP WITH TIME ZONE;
