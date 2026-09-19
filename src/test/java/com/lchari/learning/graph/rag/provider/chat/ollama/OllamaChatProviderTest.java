package com.lchari.learning.graph.rag.provider.chat.ollama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lchari.learning.graph.rag.model.ChatMessage;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OllamaChatProviderTest {

    @Mock
    private Ollama ollama;

    private OllamaChatProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OllamaChatProvider(ollama, "llama3");
    }

    @Test
    void shouldReturnResponseWhenChatIsSuccessful() throws OllamaException {
        // Arrange
        List<ChatMessage> messages = List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello")
        );

        String expectedResponse = "Hi there!";
        
        Ollama ollamaDeepMock = mock(Ollama.class, RETURNS_DEEP_STUBS);
        
        when(ollamaDeepMock.chat(any(), any())
            .getResponseModel()
            .getMessage()
            .getResponse())
            .thenReturn(expectedResponse);

        OllamaChatProvider providerWithDeepMock = new OllamaChatProvider(ollamaDeepMock, "llama3");

        // Act
        String result = providerWithDeepMock.chat(messages);

        // Assert
        assertEquals(expectedResponse, result);
    }

    @Test
    void shouldReturnErrorMessageWhenOllamaThrowsException() throws OllamaException {
        // Arrange
        List<ChatMessage> messages = List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello")
        );

        when(ollama.chat(any(), any())).thenThrow(new RuntimeException("Connection failed"));

        // Act
        String result = provider.chat(messages);

        // Assert
        assertEquals("Caught exception while calling Ollama API: Connection failed", result);
    }
}
