-- HNSW Index Optimization Script for pgvector
-- This script creates optimized HNSW indexes for vector similarity search

-- Drop existing index if it exists
DROP INDEX IF EXISTS vector_store_embedding_idx;

-- Create optimized HNSW index
-- Parameters:
--   m = 16: Maximum number of connections per layer (default: 16)
--           Higher values = better recall, more memory usage
--           Range: 2-100, recommended: 16-64 for most use cases
--
--   ef_construction = 200: Size of dynamic candidate list during index build (default: 64)
--                          Higher values = better index quality, slower build time
--                          Range: 4-1000, recommended: 100-400 for production
CREATE INDEX vector_store_embedding_idx 
ON vector_store 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 200);

-- Verify index creation
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'vector_store';

-- Performance tuning: Set ef_search for query time
-- This controls the size of the dynamic candidate list during search
-- Higher values = better recall, slower queries
-- Default: 40, recommended: 100-200 for high recall
-- Note: This is a session-level setting
SET hnsw.ef_search = 100;

-- Analyze table for query planner
ANALYZE vector_store;

-- Check index size
SELECT 
    pg_size_pretty(pg_relation_size('vector_store_embedding_idx')) as index_size,
    pg_size_pretty(pg_relation_size('vector_store')) as table_size;
