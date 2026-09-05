package com.lchari.learning.graph.rag.util;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {

  public List<String> chunkText(String text, int chunkSize, int overlap) {

    if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
      throw new IllegalArgumentException("Invalid chunk size or overlap");
    }

    List<String> chunks = new ArrayList<>();
    int start = 0;

    while (start < text.length()) {
      int previousWhitespace = 0;
      int leftIndex = start - overlap;

      while (leftIndex >= 0) {
        if (text.charAt(leftIndex) == ' ') {
          previousWhitespace = leftIndex;
          break;
        }
        leftIndex--;
      }

      int nextWhitespace = text.indexOf(' ', Math.min(start + chunkSize, text.length()));
      if (nextWhitespace == -1) {
        nextWhitespace = text.length();
      }

      String chunk = text.substring(previousWhitespace, nextWhitespace).strip();
      if (!chunk.isEmpty()) {
        chunks.add(chunk);
      }
      start = nextWhitespace + 1;
    }
    return List.copyOf(chunks);
  }
}
