package com.lchari.learning.graph.rag;

import static com.lchari.learning.graph.rag.util.CustomLogger.printAppConfig;
import static com.lchari.learning.graph.rag.util.Utils.ingest;
import static com.lchari.learning.graph.rag.util.Utils.requireCompatibleStoredEmbeddings;

import com.lchari.learning.graph.rag.chapters.Chapter2;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.ProviderFactory;

public class EssentialGraphRag {
  static void main() {
    AppConfig config = AppConfig.load();
    printAppConfig(config);

    ProviderFactory providerFactory = new ProviderFactory(config);

    var embeddingProvider = providerFactory.embeddingProvider(config.embeddingModelProfile());
    var chatProvider = providerFactory.chatProvider(config.llmModelProfile());

    try(var repository = new Neo4jRagRepository(config.neo4jConfig())) {
      EmbeddingIndexMetadata metadata;

      if(config.chapter2Config().ingest()) {
        metadata = ingest(config, embeddingProvider, repository);
      } else {
        metadata = requireCompatibleStoredEmbeddings(config, repository);
      }

      Chapter2.doExercise(config, embeddingProvider, chatProvider, repository);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
