package com.east.springairaglab.ingestion.splitter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaCodeSplitterTest {

    @Test
    void shouldSplitJavaCodeByMethods() {
        // Given: A sample Java class with multiple methods
        String javaCode = """
                package com.example;

                public class Calculator {

                    public int add(int a, int b) {
                        return a + b;
                    }

                    public int subtract(int a, int b) {
                        return a - b;
                    }

                    public int multiply(int a, int b) {
                        return a * b;
                    }

                    public int divide(int a, int b) {
                        if (b == 0) {
                            throw new IllegalArgumentException("Cannot divide by zero");
                        }
                        return a / b;
                    }
                }
                """;

        Document document = new Document(javaCode);
        document.getMetadata().put("source", "Calculator.java");

        // When: Split using JavaCodeSplitter
        JavaCodeSplitter splitter = new JavaCodeSplitter();
        List<Document> chunks = splitter.apply(List.of(document));

        // Then: Should create multiple chunks
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThan(0);

        // Verify metadata is preserved
        for (Document chunk : chunks) {
            assertThat(chunk.getMetadata()).containsKey("source");
            assertThat(chunk.getMetadata()).containsKey("class_name");
            assertThat(chunk.getMetadata()).containsKey("chunk_type");
            assertThat(chunk.getMetadata().get("class_name")).isEqualTo("Calculator");
        }

        // Print chunks for manual verification
        System.out.println("\n=== Java Code Splitter Test Results ===");
        System.out.println("Total chunks created: " + chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println("\n--- Chunk " + i + " ---");
            System.out.println(chunks.get(i).getText());
            System.out.println("Metadata: " + chunks.get(i).getMetadata());
        }
    }

    @Test
    void shouldPreserveClassContext() {
        // Given: A simple Java class
        String javaCode = """
                package com.example;

                import java.util.List;

                /**
                 * A simple service class
                 */
                public class UserService {

                    private final UserRepository repository;

                    public UserService(UserRepository repository) {
                        this.repository = repository;
                    }

                    public List<User> getAllUsers() {
                        return repository.findAll();
                    }
                }
                """;

        Document document = new Document(javaCode);
        document.getMetadata().put("source", "UserService.java");

        // When
        JavaCodeSplitter splitter = new JavaCodeSplitter(500); // Smaller chunks
        List<Document> chunks = splitter.apply(List.of(document));

        // Then
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getMetadata().get("class_name")).isEqualTo("UserService");
    }
}
