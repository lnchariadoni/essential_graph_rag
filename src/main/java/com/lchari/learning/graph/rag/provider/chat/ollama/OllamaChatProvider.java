package com.lchari.learning.graph.rag.provider.chat.ollama;

import com.lchari.learning.graph.rag.model.ChatMessage;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import java.util.List;

public class OllamaChatProvider implements ChatProvider {
  private final Ollama ollamaClient;
  private final String model;

  public OllamaChatProvider(Ollama ollamaClient, String model) {
    this.ollamaClient = ollamaClient;
    this.model = model;
  }

  @Override
  public String chat(List<ChatMessage> messages) {
    OllamaChatRequest request = OllamaChatRequest.builder().withModel(model);

    messages.forEach(message -> request.withMessage(mapToOllamaRole(message.role()), message.content()));

    try {
      var result = ollamaClient.chat(request.build(), null);
      return result.getResponseModel().getMessage().getResponse();
    } catch (Exception e) { // TODO. handle exception properly
      return "Caught exception while calling Ollama API: " + e.getMessage();
    }

  }

  private static OllamaChatMessageRole mapToOllamaRole(ChatMessage.Role role) {
    return switch (role) {
      case SYSTEM -> OllamaChatMessageRole.SYSTEM;
      case USER -> OllamaChatMessageRole.USER;
      case ASSISTANT -> OllamaChatMessageRole.ASSISTANT;
    };
  }
}
