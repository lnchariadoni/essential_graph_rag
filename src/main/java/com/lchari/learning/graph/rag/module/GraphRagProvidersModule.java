package com.lchari.learning.graph.rag.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import com.lchari.learning.graph.rag.provider.chat.ollama.OllamaChatProvider;
import com.lchari.learning.graph.rag.provider.chat.openai.OpenAIChatProvider;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import com.lchari.learning.graph.rag.provider.embedding.ollama.OllamaEmbeddingProvider;
import com.lchari.learning.graph.rag.provider.embedding.openai.OpenAIEmbeddingProvider;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import io.github.ollama4j.Ollama;
import java.util.Map;
import java.util.function.Supplier;

public class GraphRagProvidersModule extends AbstractModule {

  @Provides
  @Singleton
  EmbeddingProvider provideEmbeddingProvider(AppConfig appConfig,
                                             Provider<OpenAIClient> openAIClientProvider,
                                             Provider<Ollama> openOllamaProvider
  ) {
    final AppConfig.EmbeddingModelProfile embeddingModelProfile = appConfig.embeddingModelProfile();
    final String provider = embeddingModelProfile.provider().trim().toLowerCase();
    final String model = embeddingModelProfile.model().trim().toLowerCase();

    Map<String, Supplier<EmbeddingProvider>> mapOfEmbeddingProviders = Map.of(
        "ollama", () -> new OllamaEmbeddingProvider(openOllamaProvider.get(), model),
        "openai", () -> new OpenAIEmbeddingProvider(openAIClientProvider.get(), model));


    return selectProvider(provider, mapOfEmbeddingProviders);
  }

  private <T> T selectProvider(String provider, Map<String, Supplier<T>> mapper) {
    if (mapper.containsKey(provider)) {
      return mapper.get(provider).get();
    }

    throw new IllegalArgumentException("Unsupported provider: " + provider);
  }

  @Provides
  @Singleton
  ChatProvider provideChatProvider(AppConfig appConfig,
                                   Provider<OpenAIClient> openAIClientProvider,
                                   Provider<Ollama> openOllamaProvider) {
    final AppConfig.LLMModelProfile chatModelProfile = appConfig.llmModelProfile();
    final String provider = chatModelProfile.provider().trim().toLowerCase();
    final String model = chatModelProfile.model().trim().toLowerCase();

    Map<String, Supplier<ChatProvider>> mapOfChatProviders = Map.of(
        "ollama", () -> new OllamaChatProvider(openOllamaProvider.get(), model),
        "openai", () -> new OpenAIChatProvider(openAIClientProvider.get(), model));

    return selectProvider(provider, mapOfChatProviders);
  }

  @Provides
  @Singleton
  OpenAIClient provideOpenAIClient(AppConfig appConfig) {
    var builder = OpenAIOkHttpClient.builder().fromEnv();

    if(appConfig.openAIConfig().apiKey() != null && !appConfig.openAIConfig().apiKey().isBlank()) {
      builder.apiKey(appConfig.openAIConfig().apiKey());
    }

    if(appConfig.openAIConfig().baseUrl() != null && !appConfig.openAIConfig().baseUrl().isBlank()) {
      builder.baseUrl(appConfig.openAIConfig().baseUrl());
    }

    return builder.build();
  }

  @Provides
  @Singleton
  Ollama provideOllamaClient(AppConfig appConfig) {
    var ollamaClient = new Ollama(appConfig.ollalamaConfig().baseUrl());
    ollamaClient.setRequestTimeoutSeconds(appConfig.ollalamaConfig().requestedTimeoutSeconds());
    return ollamaClient;
  }
}
