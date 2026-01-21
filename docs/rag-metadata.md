# RAG Metadata Design

## What is Metadata?

`Map<String, Object>` attached to each document chunk. Not embedded, stored separately.

```java
Document doc = new Document(
    "UserService handles authentication",  // content → embedded
    Map.of(                                 // metadata → not embedded
        "source", "/path/to/UserService.java",
        "class_name", "UserService"
    )
);
```

---

## Purpose

1. **Citation**: Track source of information
2. **Filtering**: Narrow search scope ("Java files only")
3. **Context**: Provide additional information to LLM
4. **Debugging**: Trace which documents were retrieved

---

## Our Implementation

### Base Fields (All Documents)
```java
{
    "source": "/path/to/UserService.java",     // File path
    "filename": "UserService.java",            // File name
    "file_type": "java" | "md" | "pdf"        // File type
}
```

### Chunking Fields
```java
{
    "chunk_index": 0,                          // Current chunk number
    "total_chunks": 5,                         // Total chunks from document
    "chunk_type": "java_code" | "markdown"    // Chunk type
}
```

### Java-Specific Fields
```java
{
    "class_name": "UserService",               // Class name
    "method_name": "authenticate",             // Method name (optional)
    "start_line": 45,                          // Start line number
    "end_line": 67                             // End line number
}
```

**Location**: 
- `JavaCodeSplitter.java:83-87`
- `MarkdownCodeSplitter.java:63-66`
- `PdfDocumentLoader.java:50-52`

---

## Best Practices

### Must-Have Fields
- `source` / `document_id`: Source tracking
- `timestamp`: Temporal information
- `type` / `category`: Classification

### Domain-Specific Fields
- **Code**: `class_name`, `method_name`, `language`
- **Documents**: `section`, `page`, `author`
- **Customer Support**: `priority`, `status`, `category`

### Search Optimization Fields
- **Filtering**: `file_type`, `language`, `category`
- **Ranking**: `importance`, `freshness`, `popularity`
- **Context**: `chunk_index`, `parent_id`

### Avoid
- Too many fields (>10)
- Duplicate information (already in content)
- PII (Personally Identifiable Information)

---

## Examples from Other Domains

### Legal Documents
```java
{
    "document_id": "contract_2024_001",
    "section": "Article 3: Terms and Conditions",
    "page_number": 15,
    "author": "John Doe, Esq.",
    "date": "2024-01-15",
    "category": "contract"
}
```

### Customer Support
```java
{
    "ticket_id": "TICKET-12345",
    "customer_tier": "premium",
    "product": "Enterprise Plan",
    "language": "en",
    "sentiment": "negative"
}
```

### Medical Records
```java
{
    "patient_id": "HASH_12345",
    "record_type": "clinical_note",
    "department": "cardiology",
    "date": "2024-01-15",
    "icd_code": "I21.0"
}
```

---

## Potential Improvements

### 1. Timestamps
```java
"ingested_at": "2024-01-20T14:30:00Z",
"file_modified_at": "2024-01-15T10:00:00Z"
```
**Use**: Prioritize recent documents

### 2. Importance Score
```java
"importance": 0.8  // 0.0 ~ 1.0
```
**Use**: Weight core files higher

### 3. Language Information
```java
"language": "java" | "kotlin" | "python"
```
**Use**: Multi-language codebase support

---

## Related Documents
- [hybrid-search.md](./hybrid-search.md) - How metadata is used in filtering
- [Code Implementation](../backend/src/main/java/com/east/springairaglab/ingestion/splitter/)
