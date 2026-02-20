-- Add AI-generated fields to OCR data for storing summaries, classifications, and tags
ALTER TABLE ocr_data ADD COLUMN ai_summary TEXT;
ALTER TABLE ocr_data ADD COLUMN document_type VARCHAR(50);
ALTER TABLE ocr_data ADD COLUMN ai_tags VARCHAR(500);
