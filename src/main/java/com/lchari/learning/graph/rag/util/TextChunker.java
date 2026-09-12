package com.lchari.learning.graph.rag.util;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {

  private void validateChunkArguments(int chunkSize, int overlap) {
    if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
      throw new IllegalArgumentException("Invalid chunk size or overlap");
    }
  }

  private int findPreviousWhitespace(String text, int start, int overlap) {
    int previousWhitespace = text.lastIndexOf(' ', start - overlap);
    return Math.max(previousWhitespace, 0);
  }

  private int findNextWhitespace(String text, int start, int chunkSize) {
    int searchFrom = Math.min(start + chunkSize, text.length());
    int nextWhitespace = text.indexOf(' ', searchFrom);

    return nextWhitespace == -1 ? text.length() : nextWhitespace;
  }

  private void addChunkIfNotEmpty(
      List<String> chunks,
      String text,
      int chunkStart,
      int chunkEnd
  ) {
    String chunk = text.substring(chunkStart, chunkEnd).strip();

    if (!chunk.isEmpty()) {
      chunks.add(chunk);
    }
  }

  public List<String> chunkText(String text, int chunkSize, int overlap) {
    validateChunkArguments(chunkSize, overlap);

    List<String> chunks = new ArrayList<>();

    int length = text.length();
    int start = 0;

    while (start < length) {
      int chunkStart = findPreviousWhitespace(text, start, overlap);
      int chunkEnd = findNextWhitespace(text, start, chunkSize);

      addChunkIfNotEmpty(chunks, text, chunkStart, chunkEnd);

      start = chunkEnd + 1;
    }

    return List.copyOf(chunks);
  }
}
