package com.lchari.learning.graph.rag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import com.lchari.learning.graph.rag.util.PdfDownloader;
import com.lchari.learning.graph.rag.util.TextChunker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IngestServiceTest {

  @Mock
  private PdfDownloader pdfDownloader;

  @Mock
  private TextChunker textChunker;

  @Mock
  private EmbeddingProvider embeddingProvider;

  @Mock
  private Neo4jRagRepository neo4jRagRepository;

  private IngestService ingestService;
  private AppConfig appConfig;
  private Path pdfPath;

  @BeforeEach
  void setUP() throws Exception {
    pdfPath = Files.createTempFile("ingest-test", ".pdf");

    try (PDDocument document = new PDDocument()) {
      document.addPage(new PDPage());
      document.save(pdfPath.toFile());
    }

    appConfig = new AppConfig(
        new AppConfig.OllamaConfig("http://localhost:11434", 60L),
        new AppConfig.OpenAIConfig("https://api.openai.com/v1", "key"),
        new AppConfig.EmbeddingModelProfile("default", "openai", "text-embedding-3-small"),
        new AppConfig.LLMModelProfile("default", "openai", "gpt-4o"),
        new AppConfig.Neo4jConfig("neo4j://localhost:7687", "user", "pass", "neo4j"),
        new AppConfig.Chapter2Config(
            "https://example.com/test.pdf",
            pdfPath,
            1000,
            100,
            5,
            "vector-index",
            "fulltext-index",
            true,
            "What is relativity?"
        )
    );

    ingestService = new IngestService(pdfDownloader, textChunker, embeddingProvider, neo4jRagRepository);

    when(pdfDownloader.downloadIfMissing(anyString(), any(Path.class))).thenReturn(pdfPath);
  }

  @AfterEach
  void cleanUp() throws Exception {
    Files.deleteIfExists(pdfPath);
  }

  @Test
  void ingestHappyPathStoresChunksAndSavesMetadata() throws Exception {
    List<String> chunks = List.of("chunk one", "chunk two");
    List<List<Double>> embeddings = List.of(
        List.of(0.1, 0.2),
        List.of(0.3, 0.4)
    );


    when(textChunker.chunkText(anyString(), eq(1000), eq(100))).thenReturn(chunks);
    when(embeddingProvider.getEmbeddings(chunks)).thenReturn(embeddings);
    when(neo4jRagRepository.firstChunk()).thenReturn(new Neo4jRagRepository.StoredChunk("chunk one", List.of(0.1, 0.2)));

    EmbeddingIndexMetadata metadata = ingestService.ingest(appConfig);

    assertEquals("vector-index", metadata.indexName());
    assertEquals("default", metadata.profile());
    assertEquals("openai", metadata.provider());
    assertEquals("text-embedding-3-small", metadata.model());
    assertEquals(2, metadata.dimensions());

    verify(neo4jRagRepository).resetChapter2Data("vector-index", "fulltext-index");
    verify(neo4jRagRepository).createVectorIndex("vector-index", "Chapter2Chunk", "embedding", 2);
    verify(neo4jRagRepository).createFulltextIndex("fulltext-index", "Chapter2Chunk", "text");
    verify(neo4jRagRepository, times(2)).awaitIndex(any());
    verify(neo4jRagRepository).storeChunks(chunks, embeddings);
    verify(neo4jRagRepository).saveMetadata(metadata);
  }

  @Test
  void ingestTHrowsWhenEmbeddingProviderReturnsNoEmbeddings() {
    List<String> chunks = List.of("chunk one");

    when(textChunker.chunkText(anyString(), eq(1000), eq(100))).thenReturn(chunks);
    when(embeddingProvider.getEmbeddings(chunks)).thenReturn(List.of());

    assertThrows(IllegalStateException.class, () -> ingestService.ingest(appConfig));

    verify(neo4jRagRepository, never()).resetChapter2Data(anyString(), anyString());
  }

  @Test
  void ingestThrowsWhenEmbeddingsSizeDoesNotMatchChunkSize() {
    List<String> chunks = List.of("chunk one", "chunk two");
    List<List<Double>> embeddings = List.of(List.of(0.1));

    when(textChunker.chunkText(anyString(), eq(1000), eq(100))).thenReturn(chunks);
    when(embeddingProvider.getEmbeddings(chunks)).thenReturn(embeddings);

    assertThrows(IllegalStateException.class, () -> ingestService.ingest(appConfig));

    verify(neo4jRagRepository, never()).resetChapter2Data(anyString(), anyString());
  }
}
