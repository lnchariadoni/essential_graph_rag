package com.lchari.learning.graph.rag.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import com.lchari.learning.graph.rag.provider.chat.ollama.OllamaChatProvider;
import com.lchari.learning.graph.rag.provider.embedding.ollama.OllamaEmbeddingProvider;
import com.lchari.learning.graph.rag.provider.chat.openai.OpenAIChatProvider;
import com.lchari.learning.graph.rag.provider.embedding.openAI.OpenAIEmbeddingProvider;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import io.github.ollama4j.Ollama;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

public class GraphRagModule extends AbstractModule {

  @Provides
  @Singleton
  AppConfig provideAppConfig() {
    return AppConfig.load();
  }

  @Provides
  @Singleton
  AppConfig.Neo4jConfig provideNeo4jConfig(AppConfig appConfig) {
    return appConfig.neo4jConfig();
  }

  @Provides
  @Singleton
  Driver provideNeo4JDriver(AppConfig.Neo4jConfig neo4jConfig) {
    Driver driver = GraphDatabase
        .driver(
            neo4jConfig.url(),
            AuthTokens.basic(neo4jConfig.username(), neo4jConfig.password())
        );

    Runtime.getRuntime().addShutdownHook(new Thread(driver::close));

    return driver;
  }

  @Provides
  @Singleton
  EmbeddingProvider provideEmbeddingProvider(AppConfig appConfig,
                                             Provider<OpenAIClient> openAIClientProvider,
                                             Provider<Ollama> openOllamaProvider
                                             ) {
    final AppConfig.EmbeddingModelProfile embeddingModelProfile = appConfig.embeddingModelProfile();
    final String provider = embeddingModelProfile.provider().trim().toLowerCase();
    final String model = embeddingModelProfile.model().trim().toLowerCase();

    return switch (provider) {
      case "ollama" -> new OllamaEmbeddingProvider(openOllamaProvider.get(), model);
      case "openai" -> new OpenAIEmbeddingProvider(openAIClientProvider.get(), model);
      default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
    };
  }

  @Provides
  @Singleton
  ChatProvider providechatProvider(AppConfig appConfig,
                            Provider<OpenAIClient> openAIClientProvider,
                            Provider<Ollama> openOllamaProvider) {
    final AppConfig.LLMModelProfile chatModelProfile = appConfig.llmModelProfile();
    final String provider = chatModelProfile.provider().trim().toLowerCase();
    final String model = chatModelProfile.model().trim().toLowerCase();

    return switch(provider) {
      case "ollama" -> new OllamaChatProvider(openOllamaProvider.get(), chatModelProfile.model());
      case "openai" -> new OpenAIChatProvider(openAIClientProvider.get(), chatModelProfile.model());
      default -> throw new IllegalArgumentException("Unsupported provider: " + chatModelProfile.provider());
    };
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
  Ollama getOllamaClient(AppConfig appConfig) {
    var ollamaClient = new Ollama(appConfig.ollalamaConfig().baseUrl());
    ollamaClient.setRequestTimeoutSeconds(appConfig.ollalamaConfig().requestedTimeoutSeconds());
    return ollamaClient;
  }
}
