package com.lchari.learning.graph.rag.provider.chat.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.lchari.learning.graph.rag.model.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAIChatProviderTest {

    @Mock
    private OpenAIClient openAIClient;

    private OpenAIChatProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OpenAIChatProvider(openAIClient, "gpt-4o");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnResponseWhenChatIsSuccessful() throws Exception {
        // Arrange
        List<ChatMessage> messages = List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello")
        );
        String expectedResponse = "Hi there!";

        OpenAIClient deepMockClient = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
        ChatCompletion chatCompletion = mock(ChatCompletion.class, RETURNS_DEEP_STUBS);
        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class, RETURNS_DEEP_STUBS);

        when(deepMockClient.chat().completions().create(any(ChatCompletionCreateParams.class)))
            .thenReturn(chatCompletion);
        when(chatCompletion.choices()).thenReturn(List.of(choice));
        when(choice.message().content()).thenReturn(Optional.of(expectedResponse));

        OpenAIChatProvider providerWithDeepMock = new OpenAIChatProvider(deepMockClient, "gpt-4o");

        // Act
        String result = providerWithDeepMock.chat(messages);

        // Assert
        assertEquals(expectedResponse, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldThrowExceptionWhenOpenAIReturnsNoContent() throws Exception {
        // Arrange
        List<ChatMessage> messages = List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello")
        );

        OpenAIClient deepMockClient = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
        ChatCompletion chatCompletion = mock(ChatCompletion.class, RETURNS_DEEP_STUBS);
        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class, RETURNS_DEEP_STUBS);

        when(deepMockClient.chat().completions().create(any(ChatCompletionCreateParams.class)))
            .thenReturn(chatCompletion);
        when(chatCompletion.choices()).thenReturn(List.of(choice));
        when(choice.message().content()).thenReturn(Optional.empty());

        OpenAIChatProvider providerWithDeepMock = new OpenAIChatProvider(deepMockClient, "gpt-4o");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> providerWithDeepMock.chat(messages));
    }

    @Test
    void shouldThrowExceptionWhenOpenAIClientThrowsException() {
        // Arrange
        List<ChatMessage> messages = List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello")
        );

        OpenAIClient deepMockClient = mock(OpenAIClient.class, RETURNS_DEEP_STUBS);
        when(deepMockClient.chat().completions().create(any(ChatCompletionCreateParams.class)))
            .thenThrow(new RuntimeException("OpenAI error"));

        OpenAIChatProvider providerWithDeepMock = new OpenAIChatProvider(deepMockClient, "gpt-4o");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> providerWithDeepMock.chat(messages));
    }
}
