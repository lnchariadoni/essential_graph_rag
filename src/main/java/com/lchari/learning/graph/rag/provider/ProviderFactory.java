package com.lchari.learning.graph.rag.provider;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.provider.ollama.OllamaChatProvider;
import com.lchari.learning.graph.rag.provider.ollama.OllamaEmbeddingProvider;
import com.lchari.learning.graph.rag.provider.openAI.OpenAIChatProvider;
import com.lchari.learning.graph.rag.provider.openAI.OpenAIEmbeddingProvider;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import io.github.ollama4j.Ollama;
import java.util.Optional;

public final class ProviderFactory {
  private final AppConfig appConfig;
  private OpenAIClient openAIClient;
  private Ollama ollamaClient;

  public ProviderFactory(AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  public EmbeddingProvider embeddingProvider(AppConfig.EmbeddingModelProfile embeddingModelProfile) {
    return switch (embeddingModelProfile.provider().trim().toLowerCase()) {
      case "openai" -> new OpenAIEmbeddingProvider(getOpenAIClient(), embeddingModelProfile.model());
      case "ollama" -> new OllamaEmbeddingProvider(getOllamaClient(), embeddingModelProfile.model());
      default -> throw new IllegalArgumentException("Unsupported provider: " + embeddingModelProfile.provider());
    };
  }

  public ChatProvider chatProvider(AppConfig.LLMModelProfile chatModelProfile) {
    return switch(chatModelProfile.provider().trim().toLowerCase()) {
      case "ollama" -> new OllamaChatProvider(getOllamaClient(), chatModelProfile.model());
      case "openai" -> new OpenAIChatProvider(getOpenAIClient(), chatModelProfile.model());
      default -> throw new IllegalArgumentException("Unsupported provider: " + chatModelProfile.provider());
    };
  }

  private OpenAIClient getOpenAIClient() {
    return Optional.ofNullable(openAIClient).orElseGet(this::buildAndGetOpenAIClient);
  }

  private OpenAIClient buildAndGetOpenAIClient() {
    var builder = OpenAIOkHttpClient.builder().fromEnv();

    buildBaseURL(builder);
    buildAPIKey(builder);

    openAIClient = builder.build();
    return openAIClient;
  }

  private void buildAPIKey(OpenAIOkHttpClient.Builder builder) {
    if(appConfig.openAIConfig().apiKey() != null && !appConfig.openAIConfig().apiKey().isBlank()) {
      builder.apiKey(appConfig.openAIConfig().apiKey());
    }
  }

  private void buildBaseURL(OpenAIOkHttpClient.Builder builder) {
    if(appConfig.openAIConfig().baseUrl() != null && !appConfig.openAIConfig().baseUrl().isBlank()) {
      builder.baseUrl(appConfig.openAIConfig().baseUrl());
    }
  }

  private Ollama getOllamaClient() {
    return Optional.ofNullable(ollamaClient).orElseGet(
        () -> {
          ollamaClient = new Ollama(appConfig.ollalamaConfig().baseUrl());
          ollamaClient.setRequestTimeoutSeconds(appConfig.ollalamaConfig().requestedTimeoutSeconds());
          return ollamaClient;
        }
    );
  }
}
