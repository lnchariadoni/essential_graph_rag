package com.lchari.learning.graph.rag.provider.embedding.ollama;

import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.embed.OllamaEmbedRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OllamaEmbeddingProvider implements EmbeddingProvider {
  private static final Logger logger = LoggerFactory.getLogger(OllamaEmbeddingProvider.class);

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
      logger.error("Ollama embedding call filed", e);
      return List.of();
    }
  }
}
