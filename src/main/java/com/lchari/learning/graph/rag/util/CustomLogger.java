package com.lchari.learning.graph.rag.util;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CustomLogger {
  private static final Logger logger = LoggerFactory.getLogger(CustomLogger.class);

  private CustomLogger() {}

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

    logger.info(embedding);
    logger.info(llm);
    logger.info(neo4j);
  }

  public static void printResults(List<RetrievedChunk> results) {
    logger.info("Retrieved Chunks:");
    results.forEach(
        chunk -> {
          String message = String.format(
              "----- %n score=%.4f, chunk-%d%n text=%s %n -----",
              chunk.score(), chunk.index(), chunk.text()
          );
          logger.info(message);
        }
    );
  }
}
