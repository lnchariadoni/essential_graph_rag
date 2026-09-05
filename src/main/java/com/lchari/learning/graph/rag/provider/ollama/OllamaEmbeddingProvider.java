package com.lchari.learning.graph.rag.provider.ollama;

import com.lchari.learning.graph.rag.provider.EmbeddingProvider;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.embed.OllamaEmbedRequest;
import java.util.List;

public class OllamaEmbeddingProvider implements EmbeddingProvider {
  private final Ollama ollama;
  private final String model;

  public OllamaEmbeddingProvider(Ollama ollama, String model) {
    this.ollama = ollama;
    this.model = model;
  }

  @Override
  public List<List<Double>> getEmbeddings(List<String> texts) {
    if(texts.isEmpty()) {
      return List.of();
    }

    try {
      var request = new OllamaEmbedRequest(model, texts);
      return ollama.embed(request).getEmbeddings();
    }
    catch (Exception e) { // TODO. handle exception properly
      return List.of();
    }
  }
}
