package com.lchari.learning.graph.rag.util;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.ChatMessage;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(Utils.class);

  private Utils() {}

  public static List<Double> firstValues(List<Double> values, int n) {
    return values.subList(0, Math.min(n, values.size()));
  }

  public static String logPreview(String text, int maxCharacters) {
    String oneLine = text.replaceAll("\\s+", " ").strip();
    String result = oneLine.length() <= maxCharacters ? oneLine : oneLine.substring(0, maxCharacters) + "...";

    logger.info("First {} characters of downloaded file:[{}]", maxCharacters, result);
    return result;
  }

  public static EmbeddingIndexMetadata requireCompatibleStoredEmbeddings(
      AppConfig appConfig,
      Neo4jRagRepository repository) {

    EmbeddingIndexMetadata metadata = repository.getMetadata(appConfig.chapter2Config().vectorIndex());

    if (metadata == null) {
      throw new IllegalArgumentException("""
          No Chapter 2 embedding metadata was found in Neo4j.
          Set chapter.ingest=true and run the exercise once.
          """);
    }

    var active = appConfig.embeddingModelProfile();
    boolean isCompatible = metadata.profile().equals(active.name())
        && metadata.provider().equalsIgnoreCase(active.provider())
        && metadata.model().equalsIgnoreCase(active.model());

    if(isCompatible) {
      logger.info("Reusing compatible embeddings already stored in Neo4j.");
      return metadata;
    } else {
      throw new IllegalStateException("""
          Stored embeddings do not match the active embedding profile.
          
          Stored: profile=%s, provider=%s, model=%s, dimensions=%d
          Active: profile=%s, provider=%s, model=%s
          
          Embeddings from different models must not be mixed.
          Set chapter2.ingest=true to regenerate the chunks and vector index.
          """.formatted(
          metadata.profile(), metadata.provider(), metadata.model(), metadata.dimensions(),
          active.name(), active.provider(), active.model())
      );
    }
  }

  public static String answerQuestion(
      ChatProvider chatProvider,
      String question,
      List<RetrievedChunk> retrievedChunks) {
    String documents = retrievedChunks
        .stream()
        .map(RetrievedChunk::text)
        .collect(Collectors.joining("\n\n---\n\n"));

    // TODO. pass this are parameter
    String systemMessage = """
        You are an enstein expert. but can only use provided documents to answer the question.
        If the documents do not contain the answer, say that the provided dcouments do not contain enough information.
        """;

    String userMessage = """
        Use the following documents to answer the question.
        ----
        Documents: %s
        ----
        Question: %s
        ----
        """.formatted(documents, question);

    return chatProvider.chat(List.of(
        ChatMessage.system(systemMessage),
        ChatMessage.user(userMessage)
    ));
  }

  public static void ensureDimensionMatches(EmbeddingIndexMetadata metadata, int queryDimensions) {
    if(metadata.dimensions() != queryDimensions)
      throw new IllegalArgumentException(String.format("Query embedding has: %d dimensions, but the stored index expects: %d. Re-ingestion is required.",
          queryDimensions, metadata.dimensions()));
  }
}
