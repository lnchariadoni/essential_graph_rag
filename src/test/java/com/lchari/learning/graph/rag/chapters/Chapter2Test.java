package com.lchari.learning.graph.rag.chapters;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Chapter2Test {

  @Mock
  private EmbeddingProvider embeddingProvider;

  @Mock
  private ChatProvider chatProvider;

  @Mock
  private Neo4jRagRepository neo4jRagRepository;

  private AppConfig appConfig;
  private Chapter2 chapter2;

  private static final EmbeddingIndexMetadata METADATA =
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
        new AppConfig.Neo4jConfig("neo4j://localhost:7687", "user", "pass", "neo4j"),
        new AppConfig.Chapter2Config(
            "https://example.com/test.pdf",
            Path.of("data/text.pdf"),
            1000,
            100,
            5,
            "vector-index",
            "fulltext-index",
            false,
            "What is relativity?"
        )
    );

    chapter2 = new Chapter2(appConfig, embeddingProvider, chatProvider, neo4jRagRepository);
  }

  @Test
  void doExerciseRunsVectorAndHybridSearchesAndAnswerBoth() {
    when(neo4jRagRepository.getMetadata("vector-index")).thenReturn(METADATA);
    when(embeddingProvider.getEmbeddings(List.of("What is relativity?"))).thenReturn(List.of(List.of(0.1, 0.2, 0.3)));

    List<RetrievedChunk> vectorResults = List.of(new RetrievedChunk(0, "vector chunk", 0.9));

    when(neo4jRagRepository.vectorSearch(
        "vector-index",
        List.of(0.1, 0.2, 0.3),
        5)
    ).thenReturn(vectorResults);

    when(neo4jRagRepository.hybridSearch(
        "vector-index",
        "fulltext-index",
        List.of(0.1, 0.2, 0.3),
        "What is relativity?",
        5)
    ).thenReturn(vectorResults);

    when(chatProvider.chat(anyList())).thenReturn("an answer");

    chapter2.doExercise();

    verify(neo4jRagRepository).vectorSearch("vector-index",
        List.of(0.1, 0.2, 0.3),
        5);

    verify(neo4jRagRepository).hybridSearch("vector-index",
        "fulltext-index",
        List.of(0.1, 0.2, 0.3),
        "What is relativity?",
        5
    );

    verify(chatProvider, times(2)).chat(anyList());
  }

  @Test
  void doExerciseThrowsWhenNoStoredMetadata() {
    when(neo4jRagRepository.getMetadata("vector-index")).thenReturn(null);
    assertThrows(IllegalArgumentException.class, chapter2::doExercise);
  }

  @Test
  void doExerciseThrowsWhenQueryDimensionsDoNotMatchStoredDimensions() {
    when(neo4jRagRepository.getMetadata("vector-index")).thenReturn(METADATA);
    when(embeddingProvider.getEmbeddings(List.of("What is relativity?"))).thenReturn(List.of(List.of(0.1, 0.2)));

    assertThrows(IllegalArgumentException.class, chapter2::doExercise);
  }
}
