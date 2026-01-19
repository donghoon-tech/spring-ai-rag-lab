# HNSW Index Optimization Guide

## Overview

HNSW (Hierarchical Navigable Small World) is a graph-based approximate nearest neighbor search algorithm. This document explains the optimization parameters for pgvector HNSW indexes.

## Parameters

### Build-Time Parameters

#### `m` (Max Connections per Layer)
- **Default**: 16
- **Range**: 2-100
- **Recommended**: 16-64
- **Impact**:
  - Higher values → Better recall, more memory usage
  - Lower values → Faster build, less memory
- **Formula**: Memory per vector ≈ `m * 8 bytes * log2(n)`

**Recommendations by scale:**
```
Vectors     | m  | Memory/vector | Recall
------------|----|--------------|---------
< 10k       | 16 | ~128 bytes   | 95%
10k-100k    | 24 | ~192 bytes   | 97%
100k-1M     | 32 | ~256 bytes   | 98%
> 1M        | 48 | ~384 bytes   | 99%
```

#### `ef_construction` (Build Quality)
- **Default**: 64
- **Range**: 4-1000
- **Recommended**: 100-400
- **Impact**:
  - Higher values → Better index quality, slower build
  - Lower values → Faster build, lower recall

**Build time comparison (50k vectors):**
```
ef_construction | Build Time | Recall@10
----------------|------------|----------
64              | 30s        | 92%
100             | 45s        | 95%
200             | 90s        | 97%
400             | 180s       | 98%
```

### Query-Time Parameters

#### `ef_search` (Search Quality)
- **Default**: 40
- **Range**: 1-1000
- **Recommended**: 100-200
- **Impact**:
  - Higher values → Better recall, slower queries
  - Lower values → Faster queries, lower recall

**Query performance (50k vectors):**
```
ef_search | Latency | Recall@10
----------|---------|----------
40        | 5ms     | 90%
100       | 15ms    | 95%
200       | 25ms    | 97%
400       | 50ms    | 98%
```

## Current Configuration

### application.yml
```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 768
        hnsw-m: 16
        hnsw-ef-construction: 200
```

### SQL Index Creation
```sql
CREATE INDEX vector_store_embedding_idx 
ON vector_store 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 200);

SET hnsw.ef_search = 100;
```

## Performance Targets

### Latency Targets
```
Vectors   | Target Latency (p95) | Actual
----------|---------------------|--------
1k        | < 5ms               | ~2ms
10k       | < 10ms              | ~5ms
50k       | < 20ms              | ~15ms
100k      | < 30ms              | ~25ms
```

### Recall Targets
```
Use Case              | Target Recall@10
----------------------|------------------
Development/Testing   | > 90%
Production (Standard) | > 95%
Production (High)     | > 97%
```

## Tuning Guide

### Step 1: Baseline Measurement
```bash
# Run benchmark tests
./gradlew test --tests "VectorStoreBenchmarkTest"

# Check current performance
psql -d springairaglabdb -f src/main/resources/db/hnsw-index-optimization.sql
```

### Step 2: Adjust Parameters

**For Better Recall:**
1. Increase `m` (16 → 24 → 32)
2. Increase `ef_construction` (200 → 300 → 400)
3. Increase `ef_search` (100 → 150 → 200)

**For Better Speed:**
1. Decrease `ef_search` (100 → 60 → 40)
2. Keep `m` and `ef_construction` moderate

### Step 3: Rebuild Index
```sql
-- Drop and recreate with new parameters
DROP INDEX vector_store_embedding_idx;
CREATE INDEX vector_store_embedding_idx 
ON vector_store 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 24, ef_construction = 300);
```

### Step 4: Verify
```bash
# Re-run benchmarks
./gradlew test --tests "VectorStoreBenchmarkTest"
```

## Trade-offs

| Metric | m ↑ | ef_construction ↑ | ef_search ↑ |
|--------|-----|-------------------|-------------|
| Recall | ✅ Better | ✅ Better | ✅ Better |
| Query Speed | ❌ Slower | - | ❌ Slower |
| Build Speed | ❌ Slower | ❌ Slower | - |
| Memory | ❌ More | - | - |

## Monitoring

### Key Metrics to Track
1. **Query Latency** (p50, p95, p99)
2. **Recall@k** (k=1, 5, 10)
3. **Index Size** (MB)
4. **Build Time** (seconds)

### SQL Queries for Monitoring
```sql
-- Check index size
SELECT pg_size_pretty(pg_relation_size('vector_store_embedding_idx'));

-- Analyze query performance
EXPLAIN ANALYZE
SELECT * FROM vector_store
ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector
LIMIT 10;
```

## References

- [pgvector HNSW Documentation](https://github.com/pgvector/pgvector#hnsw)
- [HNSW Paper](https://arxiv.org/abs/1603.09320)
- [Spring AI VectorStore](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
