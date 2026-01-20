-- Add full-text search support for hybrid search
-- This enables BM25-style keyword search alongside vector similarity search

-- Add tsvector column for full-text search
ALTER TABLE vector_store ADD COLUMN IF NOT EXISTS content_tsv tsvector;

-- Create GIN index for fast full-text search
CREATE INDEX IF NOT EXISTS vector_store_content_tsv_idx ON vector_store USING GIN (content_tsv);

-- Create trigger to automatically update tsvector when content changes
CREATE OR REPLACE FUNCTION vector_store_content_tsv_trigger() RETURNS trigger AS $$
BEGIN
  NEW.content_tsv := to_tsvector('english', COALESCE(NEW.content, ''));
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER vector_store_content_tsv_update 
  BEFORE INSERT OR UPDATE ON vector_store
  FOR EACH ROW 
  EXECUTE FUNCTION vector_store_content_tsv_trigger();

-- Populate existing rows
UPDATE vector_store SET content_tsv = to_tsvector('english', COALESCE(content, ''));
