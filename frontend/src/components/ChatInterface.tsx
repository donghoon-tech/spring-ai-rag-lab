import { useState } from 'react';
import { chatApi } from '../api/chat';
import type { ChatResponse, SourceDocument } from '../api/chat';
import './ChatInterface.css';

export function ChatInterface() {
    const [query, setQuery] = useState('');
    const [response, setResponse] = useState<ChatResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!query.trim()) return;

        setLoading(true);
        setError(null);

        try {
            const result = await chatApi.sendMessage({
                query,
                topK: 5,
                similarityThreshold: 0.5,
            });
            setResponse(result);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'An error occurred');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="chat-container">
            <div className="chat-header">
                <h1>🤖 Spring AI RAG Lab</h1>
                <p>Ask questions about your codebase</p>
            </div>

            <form onSubmit={handleSubmit} className="chat-form">
                <input
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Ask a question... (e.g., 'How does JWT authentication work?')"
                    className="chat-input"
                    disabled={loading}
                />
                <button type="submit" className="chat-button" disabled={loading}>
                    {loading ? '🔄 Thinking...' : '🚀 Ask'}
                </button>
            </form>

            {error && (
                <div className="error-message">
                    ❌ {error}
                </div>
            )}

            {response && (
                <div className="response-container">
                    <div className="answer-section">
                        <h2>💡 Answer</h2>
                        <div className="answer-content">{response.answer}</div>
                        <div className="metadata">
                            <span>📊 {response.metadata.documentsRetrieved} documents</span>
                            <span>⏱️ {response.metadata.processingTimeMs}ms</span>
                            <span>🤖 {response.metadata.model}</span>
                        </div>
                    </div>

                    {response.sources.length > 0 && (
                        <div className="sources-section">
                            <h2>📚 Sources</h2>
                            {response.sources.map((source: SourceDocument) => (
                                <div key={source.citationNumber} className="source-card">
                                    <div className="source-header">
                                        <span className="citation-number">[{source.citationNumber}]</span>
                                        <span className="source-filename">{source.filename}</span>
                                        {source.score && (
                                            <span className="source-score">
                                                {(source.score * 100).toFixed(1)}%
                                            </span>
                                        )}
                                    </div>
                                    <div className="source-meta">
                                        {source.className && <span>📦 {source.className}</span>}
                                        {source.methodName && <span>🔧 {source.methodName}</span>}
                                        {source.lineRange && <span>📍 Lines {source.lineRange}</span>}
                                    </div>
                                    <pre className="source-content">{source.content}</pre>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
