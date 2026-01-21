# Spring AI RAG Lab

Enterprise-grade RAG system specialized for technical documentation and source code. Built with **Java 21 Virtual Threads** and **Spring AI**.

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Node.js 18+
- Ollama (Local LLM)

### 1. Infrastructure
```bash
# Start PostgreSQL (pgvector)
docker run -d --name postgres-rag -e POSTGRES_USER=myuser -e POSTGRES_PASSWORD=mypassword -e POSTGRES_DB=springairaglabdb -p 5432:5432 pgvector/pgvector:pg16

# Setup Ollama
ollama serve
ollama pull llama3.2
ollama pull nomic-embed-text
```

### 2. Backend
```bash
cd backend
./gradlew bootRun
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```

Visit `http://localhost:5173` to interact with the RAG system.

---

## 🏗 Architecture

**Pipeline Flow:**
`Ingestion` -> `Chunking` -> `Vector Store` -> `Retrieval (Hybrid)` -> `Generation`

1.  **Ingestion & Chunking**:
    *   **JavaCodeSplitter**: Preserves class/method structures using regex-based parsing.
    *   **MarkdownCodeSplitter**: Hierarchical splitting by headers.
    *   **Metadata**: Attaches `className`, `methodName`, `fileType`, and `lineRange`.

2.  **Storage**:
    *   **PostgreSQL**: Stores both vectors (`embedding` column) and metadata (`jsonb`).
    *   **Indexing**: HNSW index (`m=16`, `ef_construction=200`) for performant ANN search.

3.  **Retrieval (Hybrid Search)**:
    *   **Semantic**: Cosine similarity via `pgvector`.
    *   **Keyword**: BM25-like scoring via `ts_rank_cd`.
    *   **Fusion**: Weighted combination (`α=0.7` semantic bias).

---

## ✨ Key Features

### ✅ Phase 1: Engineering Pipeline
*   **Code-Aware Ingestion**: Specialized splitters for `.java` and `.md` files to maintain semantic context.
*   **Vector Optimization**: Fine-tuned HNSW index parameters for optimal recall/latency trade-off.
*   **Extensible Factory**: Clean factory pattern to route file types (`.pdf`, `.txt`, `.yml`) to appropriate loaders.

### ✅ Phase 2: Advanced Retrieval
*   **Hybrid Search**: Overcomes semantic search limitations for exact identifiers (variable names, error codes).
*   **Metadata Filtering**: Scoped search (e.g., "Find `authenticate` in `UserService.java`").
*   **Citation Tracking**: Responses include precise source references `[1]` with line numbers.
*   **Modern UI**: React/Vite interface for real-time interaction and context visualization.

---

## 🛠 Tech Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| **Core** | Java 21, Spring Boot 3.4 | Virtual Threads for high-throughput I/O. |
| **AI Framework** | Spring AI | Standardized abstractions for RAG components. |
| **Database** | PostgreSQL + pgvector | relational metadata + vector operations in one ACID conformant DB. |
| **Models** | Ollama (Llama 3, Nomic) | Local, private, free inference for development. |
| **Frontend** | React, TypeScript, Vite | Fast, type-safe development cycle. |

---

## 📚 API Reference

### Chat
`POST /api/v1/chat`

```json
{
  "query": "How does token validation work?",
  "topK": 5,
  "similarityThreshold": 0.7,
  "filters": {
    "fileType": "java"
  }
}
```

### Ingest
`POST /api/v1/ingest?path=/absolute/path/to/project`

---

## 📝 Configuration

**Hybrid Search Tuning** (`application.yml`):
```yaml
spring.ai.rag.hybrid:
  alpha: 0.7                  # Weight for semantic score (0.0 - 1.0)
  retrieval-multiplier: 2     # Fetch 2x topK candidates before ranking
```

---

## 📂 Documentation

Detailed documentation available in `docs/`:
- [PROGRESS.md](docs/PROGRESS.md): Detailed project status and achievements.
- [rag-metadata.md](docs/rag-metadata.md): Metadata schema design.
- [hybrid-search.md](docs/hybrid-search.md): Mathematical foundation of the search algorithm.
- [retrieval-models.md](docs/retrieval-models.md): Comparison of DPR, ColBERT, and BM25.

---

**License**: MIT
