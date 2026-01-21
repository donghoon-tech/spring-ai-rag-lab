# Dense vs Sparse Representations

## Fundamental Difference

### Sparse (Lexical) - Traditional IR
```python
Document: "JWT token for authentication"

Sparse Vector (BM25, TF-IDF):
{
    "JWT": 0.8,
    "token": 0.6,
    "for": 0.2,
    "authentication": 0.7,
    ...remaining 99,996 words: 0.0
}

Dimensions: 100,000
Non-zero: 4 (99.996% sparse)
```

### Dense (Semantic) - Deep Learning
```python
Document: "JWT token for authentication"

Dense Vector (BERT, Sentence Transformers):
[0.23, -0.45, 0.67, 0.12, -0.89, ..., 0.34]

Dimensions: 768
Non-zero: 768 (100% dense)
```

---

## Characteristics

### Sparse
**Pros:**
- Interpretable: "JWT" word present with weight 0.8
- Exact matching: Keyword must match exactly
- Fast: Most values are zero (sparse operations)

**Cons:**
- No semantic understanding: "JWT" ≠ "token"
- No synonyms: "authentication" ≠ "auth"
- No context: Cannot understand word relationships

**Use Cases:**
- Legal documents (exact terms matter)
- Medical records (precise terminology)
- Compliance search (keyword-critical)

### Dense
**Pros:**
- Semantic understanding: "JWT" and "token" are related
- Synonym handling: "authentication" ≈ "auth"
- Context awareness: "for" modifies "JWT"
- Multilingual: Same semantic space

**Cons:**
- Not interpretable: What does 0.23 mean?
- Weak exact matching: May miss specific keywords
- Computationally expensive: All dimensions matter

**Use Cases:**
- Question answering (meaning matters)
- Customer support (synonyms common)
- Cross-lingual search

---

## Visual Comparison

```
Sparse (BM25):
Vocabulary: [JWT, token, auth, for, ...]
Vector:     [0.8, 0.6,   0.7,  0.2, 0, 0, 0, ...]
            ↑    ↑      ↑     ↑    rest are zeros

Dense (Semantic):
Dimensions: [dim1, dim2, dim3, ..., dim768]
Vector:     [0.23, -0.45, 0.67, ..., 0.34]
            ↑     ↑      ↑          ↑
            All dimensions have meaning (not interpretable)
```

---

## Storage and Performance

### Sparse
```
Storage: O(k) where k = non-zero elements
Typical: 10-100 non-zero per document
Search: Fast (inverted index)
```

### Dense
```
Storage: O(d) where d = dimensions (768)
Always: 768 floats per document
Search: Slower (vector similarity)
Optimization: HNSW index required
```

---

## Our Implementation

### Semantic (Dense)
```java
vectorStore.similaritySearch(query)
```
- Model: Ollama `nomic-embed-text`
- Dimensions: 768
- Distance: Cosine similarity
- Index: HNSW (m=16, ef_construction=200)

### Keyword (Sparse)
```java
keywordSearchService.search(query)
```
- Method: PostgreSQL Full-Text Search
- Ranking: `ts_rank_cd()` (BM25-like)
- Index: GIN on `tsvector`
- Language: English

---

## Comparison Table

| Aspect | Sparse (BM25) | Dense (DPR) |
|--------|---------------|-------------|
| **Representation** | Word frequency | Neural embedding |
| **Dimensions** | 100K (99% zero) | 768 (100% filled) |
| **Semantic** | ❌ | ✅✅✅ |
| **Exact Match** | ✅✅✅ | ❌ |
| **Synonyms** | ❌ | ✅✅✅ |
| **Speed** | ⚡⚡⚡ | ⚡⚡ |
| **Storage** | Small | Large |
| **Interpretable** | ✅ | ❌ |

---

## When to Use What

### Sparse Only
- Exact keywords critical (legal, medical)
- Limited compute resources
- No semantic understanding needed

### Dense Only
- Semantic understanding crucial (QA, support)
- Synonyms and multilingual common
- Exact terms less important

### Hybrid (Recommended)
- **Both matter** (code search, technical docs)
- Professional terminology + semantic understanding
- Best performance (at cost of complexity)

---

## Related Documents
- [hybrid-search.md](./hybrid-search.md) - How we combine both
- [retrieval-models.md](./retrieval-models.md) - DPR, ColBERT details
- [HNSW-OPTIMIZATION.md](./HNSW-OPTIMIZATION.md) - Dense vector indexing
