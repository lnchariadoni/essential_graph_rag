package com.lchari.learning.graph.rag.util;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import java.util.List;

public class CustomLogger {
  public static void printAppConfig(AppConfig config) {
    String embedding = String.format(
        "Embedding: profile=%s, provider=%s, model=%s",
        config.embeddingModelProfile().name(), config.embeddingModelProfile().provider(), config.embeddingModelProfile().model());

    String llm = String.format(
        "LLM: profile=%s, provider=%s, model=%s",
        config.llmModelProfile().name(), config.llmModelProfile().provider(), config.llmModelProfile().model());

    String neo4j = String.format(
        "Neo4j: url=%s, database=%s",
        config.neo4jConfig().url(), config.neo4jConfig().database());

    System.out.println(embedding);
    System.out.println(llm);
    System.out.println(neo4j);
  }

  public static void printResults(List<RetrievedChunk> results) {
    System.out.println("Retrieved Chunks:");
    results.forEach(
        chunk -> {
          String message = String.format(
              "----- %n score=%.4f, chunk-%d%n text=%s %n -----",
              chunk.score(), chunk.index(), chunk.text()
          );
          System.out.println(message);
        }
    );
  }
}
