package com.lchari.learning.graph.rag.provider.chat.ollama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lchari.learning.graph.rag.model.ChatMessage;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OllamaChatProviderTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
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
        
        when(ollama.chat(any(), any())
            .getResponseModel()
            .getMessage()
            .getResponse())
            .thenReturn(expectedResponse);

        // Act
        String result = provider.chat(messages);

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

    @Test
    void shouldMapAllRolesToTHeMatchingOllamaMessageRole() throws OllamaException {
        List<ChatMessage> messages = List.of(
            ChatMessage.system("You are a helpful assistnat"),
            ChatMessage.user("What is X?"),
            ChatMessage.assistant("X is Y")
        );

        when(ollama.chat(any(), any())
            .getResponseModel()
            .getMessage()
            .getResponse())
            .thenReturn("ok");


      ArgumentCaptor<OllamaChatRequest> requestCaptor = ArgumentCaptor.forClass(OllamaChatRequest.class);

      provider.chat(messages);

      verify(ollama, atLeastOnce()).chat(requestCaptor.capture(), any());
      var sentMessages = requestCaptor.getValue().getMessages();
      assertEquals(3, sentMessages.size());
      assertEquals(OllamaChatMessageRole.SYSTEM, sentMessages.get(0).getRole());
      assertEquals(OllamaChatMessageRole.USER, sentMessages.get(1).getRole());
      assertEquals(OllamaChatMessageRole.ASSISTANT, sentMessages.get(2).getRole());

      assertEquals(messages.get(0).content(), sentMessages.get(0).getResponse());
    assertEquals(messages.get(1).content(), sentMessages.get(1).getResponse());
    assertEquals(messages.get(2).content(), sentMessages.get(2).getResponse());


    }
}
