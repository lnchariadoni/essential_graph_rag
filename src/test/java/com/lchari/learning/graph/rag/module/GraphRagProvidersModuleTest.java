package com.lchari.learning.graph.rag.module;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Provider;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import com.lchari.learning.graph.rag.provider.chat.ollama.OllamaChatProvider;
import com.lchari.learning.graph.rag.provider.chat.openai.OpenAIChatProvider;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import com.lchari.learning.graph.rag.provider.embedding.ollama.OllamaEmbeddingProvider;
import com.lchari.learning.graph.rag.provider.embedding.openai.OpenAIEmbeddingProvider;
import com.openai.client.OpenAIClient;
import io.github.ollama4j.Ollama;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraphRagProvidersModuleTest {

  private final GraphRagProvidersModule graphRagProvidersModule = new GraphRagProvidersModule();

  private Provider<OpenAIClient> openAIClientProvider;
  private Provider<Ollama> ollamaProvider;

  private AppConfig appConfigWithEmbeddingProvider(String provider) {
    return baseConfig(provider, "openai");
  }

  private AppConfig appConfigWithChatProvider(String provider) {
    return baseConfig("openai", provider);
  }

  private AppConfig baseConfig(String embeddingProvider, String chatProvider) {
    return new AppConfig(
        new AppConfig.OllamaConfig("http://localhost:11434", 60L),
        new AppConfig.OpenAIConfig("https://api.openai.com/v1", "key"),
        new AppConfig.EmbeddingModelProfile("default", embeddingProvider, "some-model"),
        new AppConfig.LLMModelProfile("default", chatProvider, "some-model"),
        new AppConfig.Neo4jConfig("neo4j://localhost:7687", "user", "pass", "neo4j"),
        new AppConfig.Chapter2Config(
            "https://example.com/test.pdf",
            Path.of("data/test.pdf"),
            1000,
            100,
            5,
            "vector-index",
            "fulltext-index",
            false,
            "question"
        )
    );
  }

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    openAIClientProvider = mock(Provider.class);
    ollamaProvider = mock(Provider.class);

    var openAIClient = mock(OpenAIClient.class);
    var ollama = mock(Ollama.class);

    when(openAIClientProvider.get()).thenReturn(openAIClient);
    when(ollamaProvider.get()).thenReturn(ollama);
  }

  @Test
  void provideEmbeddingProviderReturnsOpenAIImplementationWhenConfigured() {
    AppConfig appConfig = appConfigWithEmbeddingProvider("openai");

    EmbeddingProvider provider = graphRagProvidersModule.provideEmbeddingProvider(appConfig,
        openAIClientProvider,
        ollamaProvider);

    assertInstanceOf(OpenAIEmbeddingProvider.class, provider);
  }

  @Test
  void provideEmbeddingProviderReturnsOllamaImplementationWhenConfigured() {
    AppConfig appConfig = appConfigWithEmbeddingProvider("ollama");

    EmbeddingProvider provider = graphRagProvidersModule.provideEmbeddingProvider(appConfig,
        openAIClientProvider,
        ollamaProvider);

    assertInstanceOf(OllamaEmbeddingProvider.class, provider);
  }

  @Test
  void provideEmbeddingProviderThrowsForUnknownProvider() {
    AppConfig appConfig = appConfigWithEmbeddingProvider("unknown");

    assertThrows(IllegalArgumentException.class,
        () -> graphRagProvidersModule.provideEmbeddingProvider(appConfig,
            openAIClientProvider,
            ollamaProvider));
  }

  @Test
  void provideChatProviderReturnsOpenAIImplementationWhenConfigured() {
    AppConfig appConfig = appConfigWithChatProvider("openai");

    ChatProvider provider = graphRagProvidersModule.provideChatProvider(appConfig,
        openAIClientProvider,
        ollamaProvider);

    assertInstanceOf(OpenAIChatProvider.class, provider);
  }

  @Test
  void provideChatProviderReturnsOllamaImplementationWhenConfigured() {
    AppConfig appConfig = appConfigWithChatProvider("ollama");

    ChatProvider provider = graphRagProvidersModule.provideChatProvider(appConfig,
        openAIClientProvider,
        ollamaProvider);

    assertInstanceOf(OllamaChatProvider.class, provider);
  }

  @Test
  void provideChatProviderThrowsForUnknownProvider() {
    AppConfig appConfig = appConfigWithChatProvider("unknown");

    assertThrows(IllegalArgumentException.class,
        () -> graphRagProvidersModule.provideChatProvider(appConfig,
            openAIClientProvider,
            ollamaProvider));
  }

  @Test
  void provideOpenAIClientBuildsClientWhenApiKeyAndBaseUrlAreSet() {
    AppConfig appConfig = new AppConfig(
        new AppConfig.OllamaConfig("http://localhost:11434", 60L),
        new AppConfig.OpenAIConfig("https://api.openai.com/v1", "key"),
        new AppConfig.EmbeddingModelProfile("default", "openai", "some-model"),
        new AppConfig.LLMModelProfile("default", "openai", "some-model"),
        new AppConfig.Neo4jConfig("neo4j://localhost:7687", "user", "pass", "neo4j"),
        new AppConfig.Chapter2Config(
            "https://example.com/test.pdf",
            Path.of("data/test.pdf"),
            1000,
            100,
            5,
            "vector-index",
            "fulltext-index",
            false,
            "question"
        )
    );

    OpenAIClient openAIClient = graphRagProvidersModule.provideOpenAIClient(appConfig);
    assertNotNull(openAIClient);
  }

  @Test
  void provideOpenAIClientBuildsClientWhenApiKeyAndBaseUrlAreBlank() {
    AppConfig appConfig = new AppConfig(
        new AppConfig.OllamaConfig("http://localhost:11434", 60L),
        new AppConfig.OpenAIConfig("", ""),
        new AppConfig.EmbeddingModelProfile("default", "openai", "some-model"),
        new AppConfig.LLMModelProfile("default", "openai", "some-model"),
        new AppConfig.Neo4jConfig("neo4j://localhost:7687", "user", "pass", "neo4j"),
        new AppConfig.Chapter2Config(
            "https://example.com/test.pdf",
            Path.of("data/test.pdf"),
            1000,
            100,
            5,
            "vector-index",
            "fulltext-index",
            false,
            "question"
        )
    );

    // As we are not setting anything in env, it should throw exception.
    assertThrows(IllegalStateException.class, () -> graphRagProvidersModule.provideOpenAIClient(appConfig));
  }

  @Test
  void provideOllamaClientBuildsClientWithConfigurationBaseUrl() {
    AppConfig appConfig = new AppConfig(
        new AppConfig.OllamaConfig("http://localhost:11434", 60L),
        new AppConfig.OpenAIConfig("https://api.openai.com/v1", "key"),
        new AppConfig.EmbeddingModelProfile("default", "ollama", "some-model"),
        new AppConfig.LLMModelProfile("default", "ollama", "some-model"),
        new AppConfig.Neo4jConfig("neo4j://localhost:7687", "user", "pass", "neo4j"),
        new AppConfig.Chapter2Config(
            "https://example.com/test.pdf",
            Path.of("data/test.pdf"),
            1000,
            100,
            5,
            "vector-index",
            "fulltext-index",
            false,
            "question"
        )
    );

    Ollama ollama = graphRagProvidersModule.provideOllamaClient(appConfig);

    assertNotNull(ollama);
  }

}
