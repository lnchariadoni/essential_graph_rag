package com.lchari.learning.graph.rag.neo4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.EagerResult;
import org.neo4j.driver.ExecutableQuery;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

@ExtendWith(MockitoExtension.class)
class Neo4jRagRepositoryTest {

  @Mock
  private Driver driver;

  @Mock
  private ExecutableQuery executableQuery;

  @Mock
  private EagerResult eagerResult;

  private Neo4jRagRepository neo4jRagRepository;

  private static final AppConfig.Neo4jConfig NEO4J_CONFIG = new AppConfig.Neo4jConfig(
      "neo4j://localhost:7687",
      "user",
      "pass",
      "neo4j"
  );

  @BeforeEach
  void setUp() {
    lenient().when(driver.executableQuery(anyString())).thenReturn(executableQuery);
    lenient().when(executableQuery.withParameters(any())).thenReturn(executableQuery);
    lenient().when(executableQuery.withConfig(any())).thenReturn(executableQuery);
    lenient().when(executableQuery.execute()).thenReturn(eagerResult);

    neo4jRagRepository = new Neo4jRagRepository(NEO4J_CONFIG, driver);
  }

  @Test
  void constructorVerifiesConnectivity() {
    verify(driver).verifyConnectivity();
  }

  @Test
  void storeChunksThrowsWhenChunksAndEmbeddingsSizesDoNotMatch() {
    List<String> chunks = List.of("a", "b");
    List<List<Double>> embeddings = List.of(List.of(0.1));

    assertThrows(IllegalArgumentException.class, () -> neo4jRagRepository.storeChunks(chunks, embeddings));
  }

  @Test
  void storeChunksExecuteQueryWHenSizesMatch() {
    List<String> chunks = List.of("a", "b");
    List<List<Double>> embeddings = List.of(List.of(0.1), List.of(0.2));

    neo4jRagRepository.storeChunks(chunks, embeddings);

    verify(driver).executableQuery(anyString());
    verify(executableQuery).withParameters(Map.of("chunks", chunks, "embeddings", embeddings));
  }

  @Test
  void getMetadataReturnsNullWhenNoRecordsFound() {
    when(eagerResult.records()).thenReturn(List.of());

    EmbeddingIndexMetadata metadata = neo4jRagRepository.getMetadata("my_index");

    assertNull(metadata);
  }

  @Test
  void getMetadataMapsRecordToMetadata() {
    Record neo4jRecord = mock(Record.class);
    Value indexNameValue = mock(Value.class);
    Value profileValue = mock(Value.class);
    Value providerValue = mock(Value.class);
    Value modelValue = mock(Value.class);
    Value dimensionsValue = mock(Value.class);

    when(neo4jRecord.get("indexName")).thenReturn(indexNameValue);
    when(indexNameValue.asString()).thenReturn("my_index");
    when(neo4jRecord.get("profile")).thenReturn(profileValue);
    when(profileValue.asString()).thenReturn("default");
    when(neo4jRecord.get("provider")).thenReturn(providerValue);
    when(providerValue.asString()).thenReturn("openai");
    when(neo4jRecord.get("model")).thenReturn(modelValue);
    when(modelValue.asString()).thenReturn("text-embedding-3-small");
    when(neo4jRecord.get("dimensions")).thenReturn(dimensionsValue);
    when(dimensionsValue.asInt()).thenReturn(1536);

    when(eagerResult.records()).thenReturn(List.of(neo4jRecord));

    EmbeddingIndexMetadata metadata = neo4jRagRepository.getMetadata("my_index");

    assertEquals(new EmbeddingIndexMetadata(
        "my_index",
        "default",
        "openai",
        "text-embedding-3-small",
        1536), metadata);
  }

  @Test
  void getMeatdataRejectsUnsafeIndexName() {
    assertThrows(IllegalArgumentException.class,
        () -> neo4jRagRepository.getMetadata("bad name"));
  }

  @Test
  void createFulltextIndexRejectsUnsafeLable() {
    assertThrows(IllegalArgumentException.class,
        () -> neo4jRagRepository.createFulltextIndex("myIndex", "bad label", "text"));
  }

  @Test
  void createFulltextIndexRejectsUnsafeProperty() {
    assertThrows(IllegalArgumentException.class,
        () -> neo4jRagRepository.createFulltextIndex("myIndex", "Chapter2Chunk", "bad;prop"));
  }

  @Test
  void createVectorIndexRejectsUnsafeLabel() {
    assertThrows(IllegalArgumentException.class,
        () -> neo4jRagRepository.createVectorIndex("myIndex", "bad label", "embedding", 128));
  }

  @Test
  void createVectorIndexExecutesWithSafeIdentifiers() {
    neo4jRagRepository.createVectorIndex("myIndex", "Chapter2Chunk", "embedding", 128);

    verify(driver).executableQuery(anyString());
  }

  @Test
  void vectorSearchMapsRecordsToRetrievedChunks() {
    Record neo4jRecord = mock(Record.class);
    Value indexValue = mock(Value.class);
    Value textValue = mock(Value.class);
    Value scoreValue = mock(Value.class);

    when(neo4jRecord.get("index")).thenReturn(indexValue);
    when(indexValue.asInt()).thenReturn(0);
    when(neo4jRecord.get("text")).thenReturn(textValue);
    when(textValue.asString()).thenReturn("chunk text");
    when(neo4jRecord.get("score")).thenReturn(scoreValue);
    when(scoreValue.asDouble()).thenReturn(0.95);

    when(eagerResult.records()).thenReturn(List.of(neo4jRecord));

    List<RetrievedChunk> results = neo4jRagRepository.vectorSearch("myIndex", List.of(0.1, 0.2), 5);
    assertEquals(List.of(new RetrievedChunk(0, "chunk text", 0.95)), results);
  }

  @Test
  void hybridSearchMapsRecordsToRetrievedChunks() {
    Record neo4jRecord = mock(Record.class);
    Value indexValue = mock(Value.class);
    Value textValue = mock(Value.class);
    Value scoreValue = mock(Value.class);

    when(neo4jRecord.get("index")).thenReturn(indexValue);
    when(indexValue.asInt()).thenReturn(1);
    when(neo4jRecord.get("text")).thenReturn(textValue);
    when(textValue.asString()).thenReturn("hybrid text");
    when(neo4jRecord.get("score")).thenReturn(scoreValue);
    when(scoreValue.asDouble()).thenReturn(0.8);

    when(eagerResult.records()).thenReturn(List.of(neo4jRecord));

    List<RetrievedChunk> results = neo4jRagRepository.hybridSearch("vecIdx",
        "fullIdx",
        List.of(0.1),
        "question",
        3);

    assertEquals(List.of(new RetrievedChunk(1, "hybrid text", 0.8)), results);
  }

  @Test
  void firstChunkThrowsWhenNoneStored() {
    when(eagerResult.records()).thenReturn(List.of());

    assertThrows(NoSuchElementException.class, () -> neo4jRagRepository.firstChunk());
  }

  @Test
  void firstChunkReturnsStoredChunk() {
    Record neo4jRecord = mock(Record.class);
    Value textValue = mock(Value.class);
    Value embeddingValue = mock(Value.class);

    when(neo4jRecord.get("text")).thenReturn(textValue);
    when(textValue.asString()).thenReturn("stored text");
    when(neo4jRecord.get("embedding")).thenReturn(embeddingValue);
    when(embeddingValue.asList(org.mockito.ArgumentMatchers.<Function<Value, Double>>any()))
        .thenReturn(List.of(0.1, 0.2));

    when(eagerResult.records()).thenReturn(List.of(neo4jRecord));

    Neo4jRagRepository.StoredChunk result = neo4jRagRepository.firstChunk();

    assertEquals("stored text", result.text());
    assertEquals(List.of(0.1, 0.2), result.embeddings());
  }

  @Test
  void dropIndexRejectsUnsafeIdentifier() {
    assertThrows(IllegalArgumentException.class, () -> neo4jRagRepository.dropIndex("bad;Index"));
  }

  @Test
  void resetChapter2DataDropsBothIndexesAndDeletesData() {
    neo4jRagRepository.resetChapter2Data("vecIdx", "fullIdx");

    ArgumentCaptor<String> cyptherCaptor = ArgumentCaptor.forClass(String.class);
    verify(driver, org.mockito.Mockito.atLeast(4))
        .executableQuery(cyptherCaptor.capture());
  }

}
