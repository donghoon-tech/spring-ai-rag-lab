-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create vector_store table
CREATE TABLE IF NOT EXISTS vector_store (
	id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
	content text,
	metadata json,
	embedding vector(768)
);

-- Create index for embedding (Default IVFFlat or HNSW without tuning)
-- Note: V2 will optimize this index
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops);
