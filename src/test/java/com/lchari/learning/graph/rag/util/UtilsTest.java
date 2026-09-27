package com.lchari.learning.graph.rag.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UtilsTest {

  @Mock
  private ChatProvider chatProvider;

  @Mock
  private Neo4jRagRepository neo4jRagRepository;

  private AppConfig appConfig;

  private static final EmbeddingIndexMetadata STORED_METADATA =
      new EmbeddingIndexMetadata("vector-index",
          "default",
          "openai",
          "text-embedding-3-small",
          3);

  @BeforeEach
  void setUp() {
    appConfig = new AppConfig(
        new AppConfig.OllamaConfig("http://localhost:11434", 60L),
        new AppConfig.OpenAIConfig("https://api.openai.com/v1", "key"),
        new AppConfig.EmbeddingModelProfile("default", "openai", "text-embedding-3-small"),
        new AppConfig.LLMModelProfile("default", "openai", "gpt-4o"),
        new AppConfig.Neo4jConfig("neo4j://locahost:7687", "user", "pass", "neo4j"),
        new AppConfig.Chapter2Config(
            "https://example.com/text.pdf",
            Path.of("data/test.pdf"),
            1000,
            100,
            5,
            "vector-index",
            "fulltext-index",
            false,
            "what is relativity?"
        )
    );
  }

  @Test
  void firstValuesReturnsFirstNWhenListLargerThanN() {
    List<Double> values = List.of(1.0, 2.0, 3.0, 4.0);

    assertEquals(List.of(1.0, 2.0), Utils.firstValues(values, 2));
  }

  @Test
  void firstValuesRetunsWholeListWhenNLargerThanListSize() {
    List<Double> values = List.of(1.0, 2.0, 3.0, 4.0);

    assertEquals(List.of(1.0, 2.0, 3.0, 4.0), Utils.firstValues(values, 200));
  }

  @Test
  void previewCollapsesWhitespaceAndTruncatesLongText() {
    String text = "hello    \n\n world this is a      long    text";

    String result = Utils.logPreview(text, 11);

    assertEquals("hello world...", result);
  }

  @Test
  void previewReturnsWholeTextWhenShorterThanMax() {
    String text = "short text";

    String result = Utils.logPreview(text, 100);

    assertEquals(text, result);
  }

  @Test
  void ensureDimensionMatchesDoesNothingWhenDimensionsMatch() {
    assertDoesNotThrow(() -> Utils.ensureDimensionMatches(STORED_METADATA, 3));
  }

  @Test
  void ensureDimensionMatchesThrowsWhenDimensionsDiffer() {
    assertThrows(IllegalArgumentException.class, () -> Utils.ensureDimensionMatches(STORED_METADATA, 5));
  }

  @Test
  void answerQuestionSendsSystemANdUserMessagesToChatProvider() {
    List<RetrievedChunk> chunks = List.of(
        new RetrievedChunk(0, "first chunk", 0.9),
        new RetrievedChunk(1, "second chunk", 0.98)
    );

    when(chatProvider.chat(anyList())).thenReturn("the answer");

    String answer = Utils.answerQuestion(chatProvider, "What is X?", chunks);

    assertEquals("the answer", answer);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(chatProvider).chat(captor.capture());
    assertEquals(2, captor.getValue().size());
  }

  @Test
  void requireCompatibleStoredEmbeddingsThrowsWhenNoMetadataStored() {
    when(neo4jRagRepository.getMetadata("vector-index")).thenReturn(null);

    assertThrows(IllegalArgumentException.class,
        () -> Utils.requireCompatibleStoredEmbeddings(appConfig, neo4jRagRepository));
  }

  @Test
  void requireCompatibleStoredEmbeddingsReturnsMetadataWhenCompatible() {
    when(neo4jRagRepository.getMetadata("vector-index"))
        .thenReturn(STORED_METADATA);

    EmbeddingIndexMetadata result = Utils.requireCompatibleStoredEmbeddings(appConfig, neo4jRagRepository);

    assertEquals(STORED_METADATA, result);
  }

  @Test
  void requireComptabileStoredEmbeddingsThrowsWhenProfileIncompatible() {
    EmbeddingIndexMetadata indexMetadata = new EmbeddingIndexMetadata(
        "vector-index",
        "other-profile",
        "ollama",
        "nomic-embed-text",
        768
    );

    when(neo4jRagRepository.getMetadata("vector-index")).thenReturn(indexMetadata);

    assertThrows(IllegalStateException.class,
        () -> Utils.requireCompatibleStoredEmbeddings(appConfig, neo4jRagRepository));
  }
}
