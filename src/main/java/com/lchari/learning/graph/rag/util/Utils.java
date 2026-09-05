package com.lchari.learning.graph.rag.util;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.ChatMessage;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.ChatProvider;
import com.lchari.learning.graph.rag.provider.EmbeddingProvider;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

  private Utils() {}

  public static List<Double> firstValues(List<Double> values, int n) {
    return values.subList(0, Math.min(n, values.size()));
  }

  public static String preview(String text, int maxCharacters) {
    String oneLine = text.replaceAll("\\s+", " ").strip();
    return oneLine.length() <= maxCharacters ? oneLine : oneLine.substring(0, maxCharacters) + "...";
  }

  public static EmbeddingIndexMetadata ingest(
      AppConfig appConfig,
      EmbeddingProvider embeddingProvider,
      Neo4jRagRepository repository) throws Exception {

    var pdf = new PdfDownloader().downloadIfMissing(
        appConfig.chapter2Config().pdfUrl(),
        appConfig.chapter2Config().pdfPath());

    String text = PdfTextExtractor.extract(pdf);
    System.out.println("First 50 characters of downloaded file:" + preview(text, 50));

    List<String> chunks = new TextChunker().chunkText(
        text,
        appConfig.chapter2Config().chunkSize(),
        appConfig.chapter2Config().chunkOverlap()
    );

    System.out.println(String.format("Generated chunks: %d, first chunk:%s", chunks.size(), chunks.getFirst()));

    List<List<Double>> embeddings = embeddingProvider.getEmbeddings(chunks);

    if(embeddings.isEmpty()) {
      throw new IllegalStateException("Embedding provider returned no embeddings");
    }

    if(embeddings.size() != chunks.size()) {
      throw new IllegalStateException(
          String.format("Expected %d embeddings but received %d", chunks.size(), embeddings.size())
      );
    }

    int dimensions = embeddings.getFirst().size();
    System.out.println(String.format("Embedding vectors:%d Dimensions:%d First 3 values:%s ",
        embeddings.size(), dimensions, firstValues(embeddings.getFirst(), 3).toString()));

    System.out.println("Writing embeddings as vectors to Nejo4j");

    repository.resetChapter2Data(
        appConfig.chapter2Config().vectorIndex(),
        appConfig.chapter2Config().fulltextIndex()
    );

    repository.createVectorIndex(appConfig.chapter2Config().vectorIndex(), "Chapter2Chunk", "embedding", dimensions);
    repository.awaitIndex(appConfig.chapter2Config().vectorIndex());

    repository.createFulltextIndex(appConfig.chapter2Config().fulltextIndex(), "Chapter2Chunk", "text");
    repository.awaitIndex(appConfig.chapter2Config().fulltextIndex());

    repository.storeChunks(chunks, embeddings);

    EmbeddingIndexMetadata metadata = new EmbeddingIndexMetadata(
        appConfig.chapter2Config().vectorIndex(),
        appConfig.embeddingModelProfile().name(),
        appConfig.embeddingModelProfile().provider(),
        appConfig.embeddingModelProfile().model(),
        dimensions
    );

    repository.saveMetadata(metadata);

    var stored = repository.firstChunk();
    System.out.println("""
        Stored chunk 0 preview of first 50 letters: %s
        Stored first 3 embedding values: %s
        """.formatted(preview(stored.text(), 50),
        firstValues(stored.embeddings(), 3).toString())
        );

    return metadata;
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
      System.out.println("Reusing compatible embeddings already stored in Neo4j.");
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
