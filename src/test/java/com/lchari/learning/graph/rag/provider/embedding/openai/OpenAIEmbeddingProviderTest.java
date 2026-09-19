package com.lchari.learning.graph.rag.provider.embedding.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.Embedding;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAIEmbeddingProviderTest {

    @Mock
    private OpenAIClient openAIClient;

    private EmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OpenAIEmbeddingProvider(openAIClient, "gpt-4o");
    }

    @Test
    void shouldReturnEmptyListWhenTextsIsEmpty() {
        // Arrange
        List<String> texts = List.of();

        // Act
        List<List<Double>> result = provider.getEmbeddings(texts);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(openAIClient);
    }

    @Test
    void shouldReturnEmbeddingsWhenTextsAreValid() throws Exception {
        // Arrange
        List<String> texts = List.of("Hello", "World");

        // Mocking the data elements with different indices to test the sorting logic
        Embedding embedding0 = mock(Embedding.class);
        when(embedding0.index()).thenReturn(0L);
        when(embedding0.embedding()).thenReturn(List.of(0.1f, 0.2f, 0.3f));

        Embedding embedding1 = mock(Embedding.class);
        when(embedding1.index()).thenReturn(1L);
        when(embedding1.embedding()).thenReturn(List.of(0.4f, 0.5f, 0.6f));

        // We provide them in reverse order to verify that the provider correctly sorts them by index
        List<Embedding> unsortedData = List.of(embedding1, embedding0);

        // We use RETURNS_DEEP_STUBS for the client to navigate the fluent API
        OpenAIClient deepMockClient = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
        when(deepMockClient.embeddings().create(any()).data()).thenReturn(unsortedData);

        OpenAIEmbeddingProvider providerWithDeepMock = new OpenAIEmbeddingProvider(deepMockClient, "gpt-4o");

        // Act
        List<List<Double>> result = providerWithDeepMock.getEmbeddings(texts);

        // Assert
        assertEquals(2, result.size());
        // Check indexing: index 0 should be first, index 1 second
        assertEquals(0.1, result.get(0).get(0), 0.001);
        assertEquals(0.4, result.get(1).get(0), 0.001);
    }

    @Test
    void shouldReturnEmptyListWhenOpenAIThrowsException() throws Exception {
        // Arrange
        List<String> texts = List.of("Hello");

        // Using a deep stub to ensure we hit the catch block in the provider
        OpenAIClient deepMockClient = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
        when(deepMockClient.embeddings().create(any())).thenThrow(new RuntimeException("OpenAI error"));

        OpenAIEmbeddingProvider providerWithDeepMock = new OpenAIEmbeddingProvider(deepMockClient, "gpt-4o");

        // Act
        List<List<Double>> result = providerWithDeepMock.getEmbeddings(texts);

        // Assert
        assertTrue(result.isEmpty());
    }
}
