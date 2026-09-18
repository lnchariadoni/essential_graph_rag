package com.lchari.learning.graph.rag.neo4j;

import com.google.inject.Inject;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryConfig;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

public final class Neo4jRagRepository {
  private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  private final Driver neo4jDriver;
  private final QueryConfig queryConfig;

  @Inject
  public Neo4jRagRepository(AppConfig.Neo4jConfig neo4jConfig, Driver neo4jDriver) {
    this.neo4jDriver = neo4jDriver;
    this.queryConfig = QueryConfig.builder().withDatabase(neo4jConfig.database()).build();
    this.neo4jDriver.verifyConnectivity();
  }

  public void resetChapter2Data(String vectorIndexName, String fulltextIndexName) {
    String vectorIndex = safeIdentifier(vectorIndexName);
    String fulltextIndex = safeIdentifier(fulltextIndexName);

    dropIndex(vectorIndex);
    dropIndex(fulltextIndex);

    execute("MATCH (c:Chapter2Chunk) DETACH DELETE c", Map.of());
    execute("MATCH (m:RagIndexMetadata) WHERE m.indexName = $indexName DELETE m", Map.of("indexName", vectorIndex));
  }

  public void awaitIndex(String indexName) {
    execute("CALL db.awaitIndex($indexName, 300)", Map.of("indexName", safeIdentifier(indexName)));
  }

  public void storeChunks(List<String> chunks, List<List<Double>> embeddings) {
    int chunkSize = chunks.size();
    int embeddingSize = embeddings.size();

    if ( chunkSize != embeddingSize) {
      throw new IllegalArgumentException(
          String.format("Chunks and embeddings does not match. Chunks size: %d, embeddings size: %d",
              chunkSize,
              embeddingSize));
    }

    String cypher = """
        WITH $chunks AS chunks, range(0, %d) AS indexes
        UNWIND indexes AS i
        WITH i, chunks[i] AS chunk, $embeddings[i] AS embedding
        MERGE (c:Chunk:Chapter2Chunk {index: i})
        SET c.text = chunk, c.embedding = embedding
        """.formatted(chunkSize-1);

    execute(cypher, Map.of("chunks", chunks, "embeddings", embeddings));
  }

  public void saveMetadata(EmbeddingIndexMetadata metadata) {
    String cypher = """
        MERGE (m:RagIndexMetadata {indexName: $indexName})
        SET m.profile = $profile,
            m.provider = $provider,
            m.model = $model,
            m.dimensions = $dimensions
        """;

    execute(cypher, Map.of(
        "indexName", metadata.indexName(),
        "profile", metadata.profile(),
        "provider", metadata.provider(),
        "model", metadata.model(),
        "dimensions", metadata.dimensions()
    ));
  }

  public EmbeddingIndexMetadata getMetadata(String indexName) {
    String cypher = """
        MATCH (m:RagIndexMetadata {indexName: $indexName})
        RETURN m.indexName AS indexName,
          m.profile AS profile, 
          m.provider AS provider, 
          m.model AS model, 
          m.dimensions AS dimensions
        """;

    List<Record> records = query(cypher, Map.of("indexName", safeIdentifier(indexName)));

    if(records.isEmpty()) {
      return null;
    }

    Record record = records.getFirst();
    return new EmbeddingIndexMetadata(
        record.get("indexName").asString(),
        record.get("profile").asString(),
        record.get("provider").asString(),
        record.get("model").asString(),
        record.get("dimensions").asInt()
    );
  }

  public List<RetrievedChunk> vectorSearch(String indexName, List<Double> queryEmbedding, int topK) {
    String cypher = """
        CALL db.index.vector.queryNodes($indexName, $topK, $queryEmbedding) 
        YIELD node AS hit, score
        RETURN hit.index AS index, hit.text AS text, score
        ORDER BY score DESC
        """;
    return query(cypher, Map.of(
        "indexName", safeIdentifier(indexName),
        "topK", topK,
        "queryEmbedding", queryEmbedding))
        .stream()
        .map(record -> new RetrievedChunk(
            record.get("index").asInt(),
            record.get("text").asString(),
            record.get("score").asDouble()))
        .toList();
  }

  // Chapter2Chunk, text
  public void createFulltextIndex(String indexName, String label, String property) {
    String safeIndexName = safeIdentifier(indexName);
    String cypher = String.format("CREATE FULLTEXT INDEX %s IF NOT EXISTS FOR (c:%s) ON EACH [c.%s]", safeIndexName, label, property);

    execute(cypher, Map.of());
  }

  // Chapter2Chunk, embedding
  public void createVectorIndex(String indexName, String label, String property, int dimensions) {
    String safeIndexName = safeIdentifier(indexName);
    String cypher = """
        CREATE VECTOR INDEX %s IF NOT EXISTS
        FOR (c:%s)
        ON (c.%s)
        OPTIONS {indexConfig: {
        `vector.dimensions`: %d,
        `vector.similarity_function`: 'cosine'
        }}
        """.formatted(safeIndexName, label, property, dimensions);

    execute(cypher, Map.of());
  }

  public List<RetrievedChunk> hybridSeach(String vectorIndexName,
                                          String fulltextIndexName,
                                          List<Double> questionEmbedding,
                                          String question,
                                          int topK) {
    String cypher = """
        CALL {
                CALL db.index.vector.queryNodes($vectorIndexName, $topK, $questionEmbedding)
                YIELD node, score
                WITH collect({node: node, score: score}) AS nodes, max(score) AS maxScore
                UNWIND nodes AS n
                RETURN n.node as node,
                  CASE WHEN maxScore = 0 THEN n.score ELSE n.score / maxScore END AS score
                  
                UNION
                
                  CALL db.index.fulltext.queryNodes($fulltextIndexName, $question, {limit: $topK})
                  YIELD node, score
                  WITH collect({node: node, score: score}) AS nodes, max(score) AS maxScore
                  UNWIND nodes AS n
                  RETURN n.node as node,
                    CASE WHEN maxScore = 0 THEN n.score ELSE n.score / maxScore END AS score
              }
              WITH node, max(score) AS score
              ORDER BY score DESC
              LIMIT $topK
              RETURN node.index AS index, node.text AS text, score
      """;

    List<Record> records = query(cypher, Map.of(
        "vectorIndexName", safeIdentifier(vectorIndexName),
        "fulltextIndexName", safeIdentifier(fulltextIndexName),
        "questionEmbedding", questionEmbedding,
        "question", question,
        "topK", topK
    ));

    return records
        .stream()
        .map(record -> new RetrievedChunk(
            record.get("index").asInt(),
            record.get("text").asString(),
            record.get("score").asDouble()))
        .toList();
  }

  public StoredChunk firstChunk() {
    String cypher = """
        MATCH (c:Chapter2Chunk {index: 0})
        RETURN c.text AS text, c.embedding AS embedding
        """;

    List<Record> records = query(cypher, Map.of());

    if(records.isEmpty()) {
      throw new NoSuchElementException("No Chapter2Chunk found with index 0");
    }

    Record record = records.getFirst();
    return new StoredChunk(
        record.get("text").asString(),
        record.get("embedding").asList(Value::asDouble)
    );
  }

  public void dropIndex(String indexName) {
    String safeIndexName = safeIdentifier(indexName);
    String cypher = String.format("DROP INDEX %s IF EXISTS", safeIndexName);

    execute(cypher, Map.of());
  }

  private void execute(String cypher, Map<String, Object> parameters) {
    neo4jDriver
        .executableQuery(cypher)
        .withParameters(parameters)
        .withConfig(queryConfig)
        .execute();
  }

  private List<Record> query(String cypher, Map<String, Object> parameters) {
    return neo4jDriver
        .executableQuery(cypher)
        .withParameters(parameters)
        .withConfig(queryConfig)
        .execute()
        .records();
  }

  private static String safeIdentifier(String identifier) {
    if(SAFE_IDENTIFIER.matcher(identifier).matches()) {
      return identifier;
    } else {
      throw new IllegalArgumentException("Unsafe identifier: " + identifier);
    }
  }

  public record StoredChunk(String text, List<Double> embeddings) {}
}
