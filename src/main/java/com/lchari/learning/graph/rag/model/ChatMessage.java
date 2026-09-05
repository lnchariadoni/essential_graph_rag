package com.lchari.learning.graph.rag.model;

public record ChatMessage(Role role, String content) {
  public static ChatMessage system(String content) { return new ChatMessage(Role.SYSTEM, content); }
  public static ChatMessage user(String content) { return new ChatMessage(Role.USER, content); }
  public static ChatMessage assistant(String content) { return new ChatMessage(Role.ASSISTANT, content); }

  public enum Role {
    SYSTEM,
    USER,
    ASSISTANT
  }
}
