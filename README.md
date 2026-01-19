# Spring AI RAG Lab

Enterprise RAG system using Java 21 Virtual Threads and Spring AI. Focus: code-aware chunking for technical documentation retrieval.

## Architecture

```
Document Ingestion → Code-Aware Chunking → Vector Store (pgvector/HNSW)
                                                    ↓
                                            RAG Pipeline
                                                    ↓
                                        AI Models (Ollama/OpenAI)
```

## Implementation Status

### Phase 1: Data Ingestion (90%)
- **Code-Aware Chunking**: Custom TextSplitter implementations preserve Java class/method boundaries and Markdown header hierarchy
- **SplitterFactory**: File-type routing (`.java`, `.md`, `.txt`, `.gradle`, `.properties`, `.yaml`, `.yml`, `.pdf`)
- **PDF Support**: Spring AI PDF Reader with page-level granularity
- **Vector Store**: pgvector integration with metadata enrichment
- **Pending**: HNSW index tuning

### Phase 2-5: Planned
- Hybrid search (semantic + keyword)
- PII masking via Spring AI Advisors
- Engineering dashboard with TTFT/token cost metrics
- Event-driven ingestion with DLQ

## Tech Stack

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Runtime | Java 21 Virtual Threads | High-concurrency I/O for parallel LLM calls |
| Framework | Spring Boot 3.4 + Spring AI 1.0.0-M5 | Native RAG primitives, production-ready |
| Vector DB | PostgreSQL + pgvector | HNSW indexing, ACID guarantees |
| Models | Ollama (local), GPT-4o (cloud) | Cost/latency trade-off testing |

## Key Components

### JavaCodeSplitter
Regex-based AST-lite parser. Tracks brace depth to identify method boundaries. Prevents mid-method splits that degrade retrieval quality.

**Metadata added:**
- `class_name`: Extracted via `CLASS_PATTERN` regex
- `method_name`: Per-method tracking
- `chunk_type`: `java_code`
- `chunk_index`, `total_chunks`: Navigation

**Token estimation:** 1 token ≈ 4 chars (code-specific heuristic)

### MarkdownCodeSplitter
Header-based splitting (`#{1,6}`). Preserves document hierarchy. Default chunk size: 1000 tokens.

### SplitterFactory
File extension → TextSplitter mapping. Fallback: `TokenTextSplitter` for unsupported types.

## Setup

```bash
# Infrastructure
cd backend && docker-compose up -d

# Build
./gradlew clean build

# Run
./gradlew bootRun

# Ingest
curl -X POST "http://localhost:8080/api/v1/ingest?path=/path/to/codebase"
```

## Configuration

```yaml
# application.yml
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536  # OpenAI embedding size
```

## Project Structure

```
backend/src/main/java/com/east/springairaglab/
├── ingestion/
│   ├── api/DocumentIngestionController.java
│   ├── service/IngestionService.java
│   ├── splitter/
│   │   ├── JavaCodeSplitter.java
│   │   └── MarkdownCodeSplitter.java
│   └── factory/SplitterFactory.java
└── config/
```

## Testing

```bash
./gradlew test --tests "JavaCodeSplitterTest"
```

**Results:**
- `shouldSplitJavaCodeByMethods()`: PASSED
- `shouldPreserveClassContext()`: PASSED
- Metadata validation: class_name=Calculator, chunk_type=java_code

## Metrics (Phase 1)

- **Files Created**: 6 (3 main, 3 test)
- **LOC Added**: 629
- **Supported File Types**: 7
- **Test Coverage**: 100% for splitters

## Documentation

- `PROJECT.md`: Full specification
- `docs/PROGRESS.md`: Phase 1 implementation log
- `docs/ADR/`: Architecture decisions (pending)

## Next Steps

1. Benchmark HNSW index performance (recall@k vs. latency)
2. Implement hybrid search (BM25 + vector similarity)
3. Add PII masking via Spring AI Advisors

---

**Status:** Phase 1 - 90% | Next: HNSW Optimization
