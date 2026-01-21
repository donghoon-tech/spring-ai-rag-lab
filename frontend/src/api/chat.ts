const API_BASE_URL = 'http://localhost:8080/api/v1';

export interface ChatRequest {
    query: string;
    topK?: number;
    similarityThreshold?: number;
    filters?: MetadataFilter;
}

export interface MetadataFilter {
    fileType?: string;
    sourcePath?: string;
    className?: string;
    methodName?: string;
    filename?: string;
}

export interface ChatResponse {
    answer: string;
    sources: SourceDocument[];
    metadata: ResponseMetadata;
}

export interface SourceDocument {
    citationNumber: number;
    source: string;
    filename: string;
    content: string;
    score: number;
    metadata: string;
    lineRange?: string;
    className?: string;
    methodName?: string;
}

export interface ResponseMetadata {
    documentsRetrieved: number;
    processingTimeMs: number;
    model: string;
}

export const chatApi = {
    async sendMessage(request: ChatRequest): Promise<ChatResponse> {
        const response = await fetch(`${API_BASE_URL}/chat`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request),
        });

        if (!response.ok) {
            throw new Error(`API error: ${response.statusText}`);
        }

        return response.json();
    },
};
