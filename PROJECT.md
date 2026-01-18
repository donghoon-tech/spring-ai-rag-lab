# Project Specification: Spring AI RAG Lab

## 1. Project Overview

**Spring AI RAG Lab** is an enterprise-grade AI-powered backend system built with **Java 21** and **Spring AI**. It is designed to assist developers with context-aware technical support and automated security audits using a **RAG (Retrieval-Augmented Generation)** architecture. The project prioritizes engineering rigor—focusing on system reliability, performance benchmarking, and cost-performance trade-offs—rather than simple chatbot functionality.

## 2. Core Objectives

* **Engineering Precision:** Move beyond "vibes" by quantifying RAG quality using metrics like Faithfulness, Relevancy, and Hit Rate.
* **Performance at Scale:** Leverage **Java 21 Virtual Threads** to handle high-concurrency I/O-bound AI API calls efficiently.
* **Resource Optimization:** Successfully run and optimize RAG using **local LLMs (Ollama/Llama 3)**, proving that engineering can overcome local hardware constraints.
* **Enterprise Security:** Implement automated **PII (Personally Identifiable Information) masking** and security policy enforcement via AOP-style AI Interceptors.
* **Operational Transparency:** Build an **Engineering Dashboard** to visualize the "inner workings" of the AI (retrieved chunks, similarity scores, and latency).

## 3. Tech Stack

* **Language:** Java 21 (Virtual Threads for high-throughput I/O)
* **Framework:** Spring Boot 3.4+, Spring AI
* **Vector Database:** PostgreSQL with **pgvector** (HNSW Indexing)
* **AI Models:** * **Local:** Ollama (Llama 3, Gemma 2) for privacy and cost-effective testing.
* **Cloud:** OpenAI (GPT-4o) for high-reasoning tasks and "Judge-LLM" evaluation.


* **Frontend (Internal Tooling):** React + Tailwind CSS (Visualizing RAG pipeline and metrics)
* **Observability:** Prometheus, Grafana, and Micrometer (Tracking TTFT, token usage, and hit rates)

## 4. Implementation Roadmap

### Phase 1: Data Ingestion & Engineering Pipeline

* **Goal:** Build a robust ETL pipeline for technical assets.
* **Deliverables:** * Document loaders for `.pdf`, `.md`, and `.java` source code.
* **Code-aware Chunking:** Specialized splitting logic that preserves Java class/method structures.
* Vector store integration with `pgvector` using HNSW for performance.



### Phase 2: Advanced Retrieval & RAG Logic

* **Goal:** Optimize the "Retrieval" quality.
* **Deliverables:**
* **Hybrid Search:** Combined Semantic (Vector) and Keyword (Full-text) search.
* **Metadata Filtering:** Narrowing search space based on project context or file types.
* **Citations:** Source metadata mapping for every AI response.



### Phase 3: AI Agent & Security Interceptors

* **Goal:** Make the AI "Actionable" and "Secure."
* **Deliverables:**
* **AI Security Advisor:** Spring AI Request/Response Advisors for PII masking.
* **Function Calling:** AI-driven tools for scanning source code for common vulnerabilities (e.g., hardcoded credentials).



### Phase 4: Benchmarking & Visualization (The "1%" Phase)

* **Goal:** Prove system efficacy through data.
* **Deliverables:**
* **Engineering Dashboard:** UI showing real-time latency (TTFT), token cost, and retrieved chunk visualization.
* **Evaluation Engine:** Automated "Judge-LLM" scoring of local model responses against a **Golden Dataset**.

### Phase 5: Reliability & Developer Experience (Bonus Engineering Depth)

* **Goal:** Demonstrate architectural robustness and developer-friendliness.
* **Deliverables:**
* **Event-Driven Ingestion:** Decouple document processing using **Spring Events (or local queue)** for fault tolerance. (Upload -> Event -> Extract -> Embed -> Store).
* **API Standardization:** Full **OpenAPI (Swagger)** specification for all RAG endpoints.
* **Resilience Patterns:** Retry mechanisms and Dead Letter Queues (DLQ) for failed embeddings (simulated via database tables locally).

## 5. Experimental Variables & Performance Tuning

AIGuardian serves as an experimental lab to test the following:

* **A. RAG Precision Tuning:** Compare **Fixed-size** vs. **Semantic** vs. **Code-aware** chunking impact on retrieval hit rates.
* **B. Concurrency Benchmarking:** Measure throughput improvements of **Virtual Threads** vs. Standard Threads during massive parallel LLM calls.
* **C. Semantic Caching:** Implement **Redis-based Semantic Caching** to reduce costs and latency for redundant queries.
* **D. Model Routing:** Dynamically route simple queries to local models (Ollama) and complex reasoning to GPT-4o.

## 6. Expected Artifacts for Portfolio

* **Quantifiable Success:** "Optimized RAG retrieval hit rate by 30% via Hybrid Search and Code-aware chunking."
* **Architectural Depth:** "Designed a secure AI-Ops pipeline using Spring AI Interceptors for PII protection."
* **Infrastructure Maturity:** "Demonstrated the trade-off between local model latency and cloud model reasoning through systematic benchmarking."
* **System Robustness:** "Implemented decoupled, event-driven document ingestion pipeline handling failures gracefully with localized DLQ."