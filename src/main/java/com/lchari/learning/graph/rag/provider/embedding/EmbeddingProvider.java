package com.lchari.learning.graph.rag.provider.embedding;

import java.util.List;

public interface EmbeddingProvider {
  List<List<Double>> getEmbeddings(List<String> texts);
}
