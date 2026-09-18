package com.lchari.learning.graph.rag.provider.embedding.openai;

import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import java.util.Comparator;
import java.util.List;

public class OpenAIEmbeddingProvider implements EmbeddingProvider {
  private final OpenAIClient openAIClient;
    private final String model;

    public OpenAIEmbeddingProvider(OpenAIClient openAIClient, String model) {
        this.openAIClient = openAIClient;
        this.model = model;
    }

    @Override
    public List<List<Double>> getEmbeddings(List<String> texts) {
      if(texts.isEmpty()) {
        return List.of();
      }

      EmbeddingCreateParams params = EmbeddingCreateParams.builder()
          .model(model)
          .inputOfArrayOfStrings(texts)
          .build();

      return openAIClient
          .embeddings()
          .create(params)
          .data()
          .stream()
          .sorted(Comparator.comparingLong(Embedding::index))
          .map(embedding ->
              embedding
                  .embedding()
                  .stream()
                  .map(Float::doubleValue)
                  .toList()
          )
          .toList();
    }
}
