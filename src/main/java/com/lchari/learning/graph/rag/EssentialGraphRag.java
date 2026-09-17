package com.lchari.learning.graph.rag;

import static com.lchari.learning.graph.rag.util.CustomLogger.printAppConfig;
import static com.lchari.learning.graph.rag.util.Utils.ingest;
import static com.lchari.learning.graph.rag.util.Utils.requireCompatibleStoredEmbeddings;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.lchari.learning.graph.rag.chapters.Chapter2;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.AppResources;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.module.GraphRagModule;

public class EssentialGraphRag {
  static void main() {
    Injector injector = Guice.createInjector(new GraphRagModule());
    AppResources appResources = injector.getInstance(AppResources.class);

    AppConfig config =  appResources.getAppConfig();

    printAppConfig(config);

    var embeddingProvider = appResources.getEmbeddingProvider();
    var chatProvider = appResources.getChatProvider();
    var repository = appResources.getNeo4jRagRepository();

    try {
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
