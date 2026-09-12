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

    createPathIfNotExists(targetPath);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build();

    HttpResponse<Path> response = httpClient.send(request,
        HttpResponse.BodyHandlers.ofFile(targetPath));

    validateResponse(targetPath, response);

    return  targetPath;
  }

  private static void validateResponse(Path targetPath, HttpResponse<Path> response) throws IOException {
    if(response.statusCode() < 200 || response.statusCode() >= 300) {
      Files.deleteIfExists(targetPath);
      throw new IOException("Failed to download file: " + response.statusCode());
    }
  }

  private static void createPathIfNotExists(Path targetPath) throws IOException {
    Path parent = targetPath.toAbsolutePath().getParent();
    if(parent != null && !Files.exists(parent)) {
      Files.createDirectories(parent);
    }
  }
}
