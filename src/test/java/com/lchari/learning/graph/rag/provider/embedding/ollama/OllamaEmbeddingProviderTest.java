package com.lchari.learning.graph.rag.provider.embedding.ollama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.embed.OllamaEmbedRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OllamaEmbeddingProviderTest {

    @Mock
    private Ollama ollama;

    private OllamaEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OllamaEmbeddingProvider(
            ollama,
            "nomic-embed-text"
        );
    }

    @Test
    void shouldReturnEmptyListWhenTextsIsEmpty() {
        // Arrange
        List<String> texts = List.of();

        // Act
        List<List<Double>> result = provider.getEmbeddings(texts);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(ollama);
    }

    @Test
    void shouldReturnEmbeddingsWhenTextsAreValid() throws OllamaException {
        // Arrange
        List<String> texts = List.of("Hello", "World");

        List<List<Double>> expectedEmbeddings = List.of(
            List.of(0.1, 0.2, 0.3),
            List.of(0.4, 0.5, 0.6)
        );

        Ollama ollama = mock(Ollama.class, RETURNS_DEEP_STUBS);

        when(ollama.embed(any(OllamaEmbedRequest.class))
            .getEmbeddings())
            .thenReturn(expectedEmbeddings);

        OllamaEmbeddingProvider provider =
            new OllamaEmbeddingProvider(ollama, "nomic-embed-text");

        // Act
        List<List<Double>> result = provider.getEmbeddings(texts);

        // Assert
        assertEquals(expectedEmbeddings, result);
    }

    @Test
    void shouldReturnEmptyListWhenOllamaThrowsException() throws OllamaException {
        // Arrange
        List<String> texts = List.of("Hello");

        when(ollama.embed(any(OllamaEmbedRequest.class)))
            .thenThrow(new RuntimeException("Ollama unavailable"));

        // Act
        List<List<Double>> result = provider.getEmbeddings(texts);

        // Assert
        assertTrue(result.isEmpty());

        verify(ollama).embed(any(OllamaEmbedRequest.class));
    }
}