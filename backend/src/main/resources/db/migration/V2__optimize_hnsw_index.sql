-- Drop the initial index to recreate with optimized parameters
DROP INDEX IF EXISTS vector_store_embedding_idx;

-- Create optimized HNSW index
-- m = 16: Balanced recall and memory usage
-- ef_construction = 200: Higher quality index construction (slower build, better search)
CREATE INDEX vector_store_embedding_idx 
ON vector_store 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 200);

-- Note: ef_search is a session-level parameter, cannot be set permanently via CREATE INDEX
-- It should be set by the application connection pool or per-transaction if needed.
-- But the index structure itself is now optimized.
