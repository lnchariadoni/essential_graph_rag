package com.lchari.learning.graph.rag.service;

import static com.lchari.learning.graph.rag.util.Utils.firstValues;
import static com.lchari.learning.graph.rag.util.Utils.logPreview;

import com.google.inject.Inject;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import com.lchari.learning.graph.rag.util.PdfDownloader;
import com.lchari.learning.graph.rag.util.PdfTextExtractor;
import com.lchari.learning.graph.rag.util.TextChunker;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IngestService {
  private static final Logger logger = LoggerFactory.getLogger(IngestService.class);

  private final PdfDownloader pdfDownloader;
  private final TextChunker textChunker;
  private final EmbeddingProvider embeddingProvider;
  private final Neo4jRagRepository neo4jRagRepository;

  @Inject
  public IngestService(PdfDownloader pdfDownloader,
                       TextChunker textChunker,
                       EmbeddingProvider embeddingProvider,
                       Neo4jRagRepository neo4jRagRepository) {
    this.pdfDownloader = pdfDownloader;
    this.textChunker = textChunker;
    this.embeddingProvider = embeddingProvider;
    this.neo4jRagRepository = neo4jRagRepository;
  }

  public EmbeddingIndexMetadata ingest(AppConfig appConfig) throws Exception {
    Path pdf = pdfDownloader.downloadIfMissing(appConfig.chapter2Config().pdfUrl(),
        appConfig.chapter2Config().pdfPath());

    String text = PdfTextExtractor.extract(pdf);
    logPreview(text, 50);

    List<String> chunks = textChunker.chunkText(
        text,
        appConfig.chapter2Config().chunkSize(),
        appConfig.chapter2Config().chunkOverlap()
    );

    logger.info("Generated chunks: {},\n first chunk:{}", chunks.size(), chunks.getFirst());

    List<List<Double>> embeddings = embeddingProvider.getEmbeddings(chunks);

    validateEmbeddingAndChunkSize(embeddings.size(), chunks.size());

    int dimensions = embeddings.getFirst().size();
    logger.info("Embedding vectors:{} Dimensions:{} First 3 values:{} ",
        embeddings.size(), dimensions, firstValues(embeddings.getFirst(), 3));

    logger.info("Writing embeddings as vectors to Nejo4j");

    neo4jRagRepository.resetChapter2Data(
        appConfig.chapter2Config().vectorIndex(),
        appConfig.chapter2Config().fulltextIndex()
    );

    neo4jRagRepository.createVectorIndex(appConfig.chapter2Config().vectorIndex(), "Chapter2Chunk", "embedding", dimensions);
    neo4jRagRepository.awaitIndex(appConfig.chapter2Config().vectorIndex());

    neo4jRagRepository.createFulltextIndex(appConfig.chapter2Config().fulltextIndex(), "Chapter2Chunk", "text");
    neo4jRagRepository.awaitIndex(appConfig.chapter2Config().fulltextIndex());

    neo4jRagRepository.storeChunks(chunks, embeddings);

    EmbeddingIndexMetadata metadata = new EmbeddingIndexMetadata(
        appConfig.chapter2Config().vectorIndex(),
        appConfig.embeddingModelProfile().name(),
        appConfig.embeddingModelProfile().provider(),
        appConfig.embeddingModelProfile().model(),
        dimensions
    );

    neo4jRagRepository.saveMetadata(metadata);

    var stored = neo4jRagRepository.firstChunk();

    if( logger.isInfoEnabled() ) {
      logger.info(" Stored chunk 0 preview of first 50 letters: {}\n Stored first 3 embedding values: {}\n",
          logPreview(stored.text(), 50),
          firstValues(stored.embeddings(), 3));
    }

    return metadata;
  }

  private static void validateEmbeddingAndChunkSize(int embeddingSize, int chunksSize) {
    if(embeddingSize == 0) {
      throw new IllegalStateException("Embedding provider returned no embeddings");
    }

    if(embeddingSize != chunksSize) {
      throw new IllegalStateException(
          String.format("Expected %d embeddings but received %d", chunksSize, embeddingSize)
      );
    }
  }
}
