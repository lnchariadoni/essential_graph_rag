package com.lchari.learning.graph.rag;

import static com.lchari.learning.graph.rag.util.CustomLogger.printAppConfig;
import static com.lchari.learning.graph.rag.util.Utils.ingest;
import static com.lchari.learning.graph.rag.util.Utils.requireCompatibleStoredEmbeddings;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.lchari.learning.graph.rag.chapters.Chapter2;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.module.GraphRagConfigModule;
import com.lchari.learning.graph.rag.module.GraphRagNeo4JModule;
import com.lchari.learning.graph.rag.module.GraphRagProvidersModule;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;

public class EssentialGraphRag {
  static void main() {
    final Injector injector = Guice.createInjector(new GraphRagConfigModule(),
        new GraphRagNeo4JModule(),
        new GraphRagProvidersModule()
        );

    final AppConfig appConfig = injector.getInstance(AppConfig.class);
    final EmbeddingProvider embeddingProvider = injector.getInstance(EmbeddingProvider.class);
    final ChatProvider chatProvider = injector.getInstance(ChatProvider.class);
    final Neo4jRagRepository neo4jRagRepository = injector.getInstance(Neo4jRagRepository.class);


    printAppConfig(appConfig);

    try {
      EmbeddingIndexMetadata metadata;

      if(appConfig.chapter2Config().ingest()) {
        metadata = ingest(appConfig, embeddingProvider, neo4jRagRepository);
      } else {
        metadata = requireCompatibleStoredEmbeddings(appConfig, neo4jRagRepository);
      }

      var chapter2 = injector.getInstance(Chapter2.class);
      chapter2.doExercise();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
