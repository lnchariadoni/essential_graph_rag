package com.lchari.learning.graph.rag.model;

import com.google.inject.Inject;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import lombok.Getter;

@Getter
public class AppResources {
  final AppConfig appConfig;
  final EmbeddingProvider embeddingProvider;
  final ChatProvider chatProvider;
  final Neo4jRagRepository neo4jRagRepository;

  @Inject
  AppResources(AppConfig appConfig,
               EmbeddingProvider embeddingProvider,
               ChatProvider chatProvider,
               Neo4jRagRepository neo4jRagRepository) {
    this.appConfig = appConfig;
    this.embeddingProvider = embeddingProvider;
    this.chatProvider = chatProvider;
    this.neo4jRagRepository = neo4jRagRepository;
  }
}
