# Retrieval Models: DPR, ColBERT, and Beyond

## Dense Passage Retrieval (DPR)

### Paper
```
"Dense Passage Retrieval for Open-Domain Question Answering"
Vladimir Karpukhin et al., Facebook AI Research
EMNLP 2020
```

### Core Idea
Embed questions and documents into the same dense vector space. Semantically similar items cluster together.

```python
Question: "How to validate JWT tokens?"
Question Vector: [0.23, -0.45, 0.67, ...]

Document 1: "JWT tokens are validated using verify() method"
Doc Vector: [0.25, -0.43, 0.69, ...] ← Close to question!

Document 2: "Database connection configuration"
Doc Vector: [-0.67, 0.89, -0.12, ...] ← Far from question
```

### Architecture
```
Question Encoder (BERT):
  Input: "How to validate JWT?"
  Output: 768-dim vector

Document Encoder (BERT):
  Input: "Validate JWT using verify()"
  Output: 768-dim vector

Similarity: cosine(question_vec, doc_vec)
```

### Training
- Contrastive learning
- Positive pairs: (question, relevant_doc)
- Negative pairs: (question, irrelevant_doc)
- Loss: Maximize similarity for positives, minimize for negatives

### Our Implementation
```java
vectorStore.similaritySearch(query)
```
- Model: `nomic-embed-text` (DPR-style)
- Embedding: 768 dimensions
- Similarity: Cosine distance
- Index: HNSW for fast search

### Pros
- Semantic understanding
- Synonym handling
- Multilingual support

### Cons
- Weak on exact keywords
- Computationally expensive
- Requires large training data

---

## ColBERT (Contextualized Late Interaction)

### Paper
```
"ColBERT: Efficient and Effective Passage Search 
via Contextualized Late Interaction over BERT"
Omar Khattab, Matei Zaharia, Stanford NLP
SIGIR 2020
```

### Core Idea
Instead of one vector per document, create one vector per token. Match at token level for better precision.

### Standard DPR
```
Document: "JWT token for authentication"
         ↓
Single Vector: [0.23, -0.45, 0.67, ...]
```

### ColBERT
```
Document: "JWT token for authentication"
         ↓
JWT:           [0.12, -0.34, 0.56, ...]
token:         [0.23, -0.45, 0.67, ...]
for:           [0.01, -0.02, 0.03, ...]
authentication: [0.45, -0.67, 0.89, ...]
```

### Matching Process
```python
Query: "JWT validation"
Query Tokens:
  JWT:        [0.13, -0.35, 0.57, ...]
  validation: [0.46, -0.68, 0.90, ...]

Matching:
  Query "JWT" ↔ Doc "JWT"           → similarity 0.98 ✅
  Query "validation" ↔ Doc "authentication" → similarity 0.85 ✅
  
Final Score = max_sim(query_tokens, doc_tokens)
```

### Architecture
```
Query Encoder:
  Input: "JWT validation"
  Output: [vec_JWT, vec_validation]

Document Encoder:
  Input: "JWT token for authentication"
  Output: [vec_JWT, vec_token, vec_for, vec_authentication]

Late Interaction:
  For each query token, find max similarity with doc tokens
  Sum all max similarities
```

### Pros
- More accurate than DPR (token-level matching)
- Semantic + precision
- Better interpretability

### Cons
- Large storage (vector per token)
- Slower search
- Complex implementation

### When to Use
- Accuracy critical
- Resources available
- Willing to trade complexity for performance

---

## BM25 (Best Matching 25)

### Background
Classic probabilistic ranking function from 1970s-1990s.

### Formula
```
BM25(D, Q) = Σ IDF(qi) × (f(qi, D) × (k1 + 1)) / 
                         (f(qi, D) + k1 × (1 - b + b × |D|/avgdl))

Where:
- f(qi, D): Term frequency of qi in document D
- |D|: Document length
- avgdl: Average document length
- k1, b: Tuning parameters (typically k1=1.2, b=0.75)
- IDF: Inverse document frequency
```

### Key Properties
- Term frequency saturation (diminishing returns)
- Document length normalization
- IDF weighting (rare terms matter more)

### Our Implementation
```sql
SELECT ts_rank_cd(content_tsv, plainto_tsquery('english', ?)) as score
FROM vector_store
WHERE content_tsv @@ plainto_tsquery('english', ?)
```

PostgreSQL's `ts_rank_cd()` is BM25-like.

---

## Comparison

| Model | Type | Granularity | Semantic | Exact | Storage | Speed |
|-------|------|-------------|----------|-------|---------|-------|
| **BM25** | Sparse | Document | ❌ | ✅✅✅ | Small | ⚡⚡⚡ |
| **DPR** | Dense | Document | ✅✅✅ | ❌ | Medium | ⚡⚡ |
| **ColBERT** | Dense | Token | ✅✅ | ✅✅ | Large | ⚡ |
| **Hybrid** | Both | Document | ✅✅ | ✅✅ | Medium | ⚡⚡ |

---

## Evolution Timeline

```
1970s-1990s: BM25
  - Probabilistic ranking
  - Still widely used baseline

2013: Word2Vec
  - First neural embeddings
  - Word-level only

2018: BERT
  - Contextual embeddings
  - Sentence/document level

2020: DPR (Facebook AI)
  - End-to-end dense retrieval
  - Outperforms BM25 on many tasks

2020: ColBERT (Stanford)
  - Token-level interaction
  - Best of both worlds

2021+: Hybrid Methods
  - Combine dense + sparse
  - Industry standard
```

---

## Our Choice: DPR + BM25 Hybrid

### Rationale
1. **Proven**: Academically validated (EMNLP 2020)
2. **Balanced**: Semantic understanding + keyword precision
3. **Practical**: Not too complex (unlike ColBERT)
4. **Effective**: α=0.7 works for most domains

### Implementation
```java
// DPR (Dense)
List<Document> semantic = vectorStore.similaritySearch(query);

// BM25 (Sparse)
List<DocumentWithScore> keyword = keywordSearch.search(query);

// Hybrid
hybridScore = 0.7 × semantic + 0.3 × keyword
```

---

## Future Directions

### 1. ColBERT Integration
- Higher accuracy
- Requires infrastructure upgrade
- Consider for Phase 3+

### 2. Learned Sparse Retrieval
- SPLADE, DeepImpact
- Neural networks for sparse vectors
- Better than BM25, faster than dense

### 3. Multi-Vector Representations
- Multiple vectors per document
- Different aspects (code, comments, docs)
- Aspect-specific retrieval

---

## References

### Papers
1. DPR: https://arxiv.org/abs/2004.04906
2. ColBERT: https://arxiv.org/abs/2004.12832
3. BM25: Robertson & Zaragoza (2009)

### Implementations
- DPR: HuggingFace `facebook/dpr-question_encoder-single-nq-base`
- ColBERT: https://github.com/stanford-futuredata/ColBERT
- BM25: Elasticsearch, PostgreSQL Full-Text Search

---

## Related Documents
- [dense-vs-sparse.md](./dense-vs-sparse.md) - Representation comparison
- [hybrid-search.md](./hybrid-search.md) - Our fusion algorithm
- [HNSW-OPTIMIZATION.md](./HNSW-OPTIMIZATION.md) - Vector index optimization
