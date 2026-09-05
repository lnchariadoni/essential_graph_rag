package com.lchari.learning.graph.rag.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PdfDownloader {

  private final HttpClient httpClient = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  public Path downloadIfMissing(String url, Path targetPath) throws IOException, InterruptedException {
    if (Files.exists(targetPath) && Files.size(targetPath) > 0) {
      return targetPath;
    }

    Path parent = targetPath.toAbsolutePath().getParent();
    if(parent != null && !Files.exists(parent)) {
      Files.createDirectories(parent);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build();

    HttpResponse<Path> response = httpClient.send(request,
        HttpResponse.BodyHandlers.ofFile(targetPath));

    if(response.statusCode() < 200 || response.statusCode() >= 300) {
      Files.deleteIfExists(targetPath);
      throw new IOException("Failed to download file: " + response.statusCode());
    }

    return  targetPath;
  }
}
