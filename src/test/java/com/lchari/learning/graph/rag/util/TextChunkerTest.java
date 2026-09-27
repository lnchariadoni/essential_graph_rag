package com.lchari.learning.graph.rag.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {
  private final TextChunker textChunker = new TextChunker();

  @Test
  void shouldReturnEmptyListForEmptyText() {
    List<String> chunks = textChunker.chunkText("", 10, 2);

    assertTrue(chunks.isEmpty());
  }

  @Test
  void shouldReturnSingleChunkWhenTextShorterThanChunkSize() {
    List<String> chunks = textChunker.chunkText("hello world", 100, 10);

    assertEquals(1, chunks.size());
    assertEquals("hello world", chunks.getFirst());
  }

  @Test
  void shouldSPlitIntoMultipleChunksRespectingWhitespaceBoundaries() {
    String text = "the quick brown fox jumps over the lazy dog";

    List<String> chunks = textChunker.chunkText(text, 10, 3);

    assertTrue(chunks.size() > 1);
    chunks.forEach(chunk -> assertFalse(chunk.isBlank(), "chunks should never be blank"));
  }

  @Test
  void shouldProduceOverlappingContentBetweenConsecutiveChunks() {
    String text = "one two three four five six seven eight nine ten";

    List<String> chunks = textChunker.chunkText(text, 15, 5);

    assertTrue(chunks.size() > 1);
  }

  @Test
  void shouldHandleTextWithNoWhitespace() {
    String text = "onetwothreefourfivesixseveneightnineten";

    List<String> chunks = textChunker.chunkText(text, 10, 2);

    assertEquals(1, chunks.size());
    assertEquals(text, chunks.getFirst());
  }

  @Test
  void shouldStripWhitespaceFromChunks() {
    String text = "       leading and trailing spaces     ";

    List<String> chunks = textChunker.chunkText(text, 100, 5);

    assertEquals(1, chunks.size());
    assertEquals("leading and trailing spaces", chunks.getFirst());
  }

  @Test
  void shouldThrowWhenChunkSizeIsZeroOrNegative() {
    assertThrows(IllegalArgumentException.class, () -> textChunker.chunkText("text", 0, 0));
    assertThrows(IllegalArgumentException.class, () -> textChunker.chunkText("text", -5, 0));
  }

  @Test
  void shouldThrowWHenOverlapIsNegative() {
    assertThrows(IllegalArgumentException.class, () -> textChunker.chunkText("text", 10, -1));
  }

  @Test
  void shouldThrowWhenOverlapIsGreaterThanOrEqualToChunkSize() {
    assertThrows(IllegalArgumentException.class, () -> textChunker.chunkText("text", 10, 10));
    assertThrows(IllegalArgumentException.class, () -> textChunker.chunkText("text", 10, 11));
  }

  @Test
  void shouldNotProduceEmptyCHunksForTextEndingWithMultipleSpaces() {
    String text = "word1 word2      ";

    List<String> chunks = textChunker.chunkText(text, 5, 2);
    chunks.forEach(chunk -> assertFalse(chunk.isEmpty()));
  }
}
