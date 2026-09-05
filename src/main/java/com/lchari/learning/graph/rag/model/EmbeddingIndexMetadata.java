package com.lchari.learning.graph.rag.model;

public record EmbeddingIndexMetadata(
    String indexName,
    String profile,
    String provider,
    String model,
    int dimensions) {
}
