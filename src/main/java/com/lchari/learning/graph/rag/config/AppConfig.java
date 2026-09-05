package com.lchari.learning.graph.rag.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.nio.file.Path;

public record AppConfig(
    OllamaConfig ollalamaConfig,
    OpenAIConfig openAIConfig,
    EmbeddingModelProfile embeddingModelProfile,
    LLMModelProfile llmModelProfile,
    Neo4jConfig neo4jConfig,
    Chapter2Config chapter2Config) {

  public static AppConfig load() {
    Config config = ConfigFactory.load();

    OllamaConfig ollamaConfig = OllamaConfig.from(config.getConfig("providers.ollama"));
    OpenAIConfig openAIConfig = OpenAIConfig.from(config.getConfig("providers.openai"));

    String activeEmbedding = config.getString("embeddings.active");
    String activeLlm = config.getString("llms.active");

    EmbeddingModelProfile embeddingModelProfile = embeddingModelProfile(config, "embeddings", activeEmbedding);
    LLMModelProfile llmModelProfile = llmModelProfile(config, "llms", activeLlm);

    Neo4jConfig neo4jConfig = Neo4jConfig.from(config.getConfig("neo4j"));
    Chapter2Config chapter2Config = Chapter2Config.from(config.getConfig("chapter2"));

    return new AppConfig(
        ollamaConfig,
        openAIConfig,
        embeddingModelProfile,
        llmModelProfile,
        neo4jConfig,
        chapter2Config
    );
  }

  private static EmbeddingModelProfile embeddingModelProfile(Config config, String section, String activeName) {
    Config profile = config.getConfig(section + ".profiles." + activeName);

    return new EmbeddingModelProfile(
        activeName,
        profile.getString("provider"),
        profile.getString("model")
    );
  }

  private static LLMModelProfile llmModelProfile(Config config, String section, String activeName) {
    Config profile = config.getConfig(section + ".profiles." + activeName);

    return new LLMModelProfile(
        activeName,
        profile.getString("provider"),
        profile.getString("model")
    );
  }

  public record OllamaConfig(String baseUrl, long requestedTimeoutSeconds) {
    public static OllamaConfig from(Config ollamaConfig) {
      return new OllamaConfig(ollamaConfig.getString("baseUrl"),
          ollamaConfig.getLong("requestedTimeoutSeconds"));
    }
  }
  public record OpenAIConfig(String baseUrl, String apiKey) {
    public static OpenAIConfig from(Config openAIConfig) {
      return new OpenAIConfig(
          openAIConfig.getString("baseUrl"),
          openAIConfig.getString("apiKey")
      );
    }
  }

  public record EmbeddingModelProfile(String name, String provider, String model) {}
  public record LLMModelProfile(String name, String provider, String model) {}

  public record Neo4jConfig(String url, String username, String password, String database) {
    public static Neo4jConfig from(Config neo4jConfig) {
      return new Neo4jConfig(
          neo4jConfig.getString("url"),
          neo4jConfig.getString("username"),
          neo4jConfig.getString("password"),
          neo4jConfig.getString("database")
      );
    }
  }

  public record Chapter2Config(String pdfUrl,
                               Path pdfPath,
                               int chunkSize,
                               int chunkOverlap,
                               int topK,
                               String vectorIndex,
                               String fulltextIndex,
                               boolean ingest,
                               String question) {
      public static Chapter2Config from(Config chapter2Config) {
        return new Chapter2Config(
            chapter2Config.getString("pdfUrl"),
            Path.of(chapter2Config.getString("pdfPath")),
            chapter2Config.getInt("chunkSize"),
            chapter2Config.getInt("chunkOverlap"),
            chapter2Config.getInt("topK"),
            chapter2Config.getString("vectorIndex"),
            chapter2Config.getString("fulltextIndex"),
            chapter2Config.getBoolean("ingest"),
            chapter2Config.getString("question")
        );
      }
  }
}
