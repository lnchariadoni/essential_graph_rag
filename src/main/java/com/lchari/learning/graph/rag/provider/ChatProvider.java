package com.lchari.learning.graph.rag.provider;

import com.lchari.learning.graph.rag.model.ChatMessage;
import java.util.List;

public interface ChatProvider {
  String chat(List<ChatMessage> messages);
}
