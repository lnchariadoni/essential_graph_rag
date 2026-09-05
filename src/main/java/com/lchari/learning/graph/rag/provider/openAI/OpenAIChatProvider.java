package com.lchari.learning.graph.rag.provider.openAI;

import com.lchari.learning.graph.rag.model.ChatMessage;
import com.lchari.learning.graph.rag.provider.ChatProvider;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import java.util.List;

public class OpenAIChatProvider implements ChatProvider {
  private final OpenAIClient openAIClient;
  private final String model;

    public OpenAIChatProvider(OpenAIClient openAIClient, String model) {
      this.openAIClient = openAIClient;
      this.model = model;
    }

    @Override
    public String chat(List<ChatMessage> messages) {
      var builder = ChatCompletionCreateParams.builder().model(model);

      for (ChatMessage message : messages) {
        switch (message.role()) {
          case SYSTEM -> builder.addSystemMessage(message.content());
          case USER -> builder.addUserMessage(message.content());
          case ASSISTANT -> builder.addAssistantMessage(message.content());
        }
      }

      var response = openAIClient.chat().completions().create(builder.build());

      return response
          .choices()
          .stream()
          .findFirst()
          .flatMap(choice -> choice.message().content())
          .orElseThrow(() -> new IllegalArgumentException("OpenAI returned no text content in the response."));
    }
}
