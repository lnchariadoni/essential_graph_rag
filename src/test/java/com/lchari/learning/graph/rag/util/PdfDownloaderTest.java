package com.lchari.learning.graph.rag.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PdfDownloaderTest {

  private HttpServer server;
  private int port;
  private final PdfDownloader pdfDownloader = new PdfDownloader();
  private Path targetDir;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    port = server.getAddress().getPort();
    targetDir = Files.createTempDirectory("pdf_downloader_test");
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void downloadsFileWhenTargetIsMissing() throws Exception {
    byte[] body = "pdf-content".getBytes(StandardCharsets.UTF_8);
    server.createContext("/file.pdf", exchange -> {
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });

    server.start();

    Path target = targetDir.resolve("nested/output.pdf");
    Path result = pdfDownloader.downloadIfMissing("http://localhost:" + port + "/file.pdf", target);

    assertEquals(target, result);
    assertTrue(Files.exists(target));
    assertEquals("pdf-content", Files.readString(target));
  }

  @Test
  void skipsDownloadWhenTargetAlreadyExistsAndIsNonEmpty() throws Exception {
    server.createContext("/file.pdf", exchange -> {
      throw new AssertionError("Server should not be called when file already exists");
    });

    server.start();

    Path target = targetDir.resolve("existing.pdf");
    Files.writeString(target, "already-there");

    Path result = pdfDownloader.downloadIfMissing("http://localhost:" + port + "/file.pdf", target);

    assertEquals(target, result);
    assertEquals("already-there", Files.readString(target));
  }

  @Test
  void deletesPartialFileAndThrowsWhenServerReturnsError() {
    server.createContext("/missing.pdf", exchange -> {
      exchange.sendResponseHeaders(404, -1);
      exchange.close();
    });

    server.start();

    Path target = targetDir.resolve("file.pdf");

    assertThrows(IOException.class,
        () -> pdfDownloader.downloadIfMissing("http://localhost:" + port + "/missing.pdf", target));

    assertFalse(Files.exists(target));
  }

  @Test
  void readownloadWhenExistingFileIsEmpty() throws Exception {
    byte[] body = "fresh-content".getBytes(StandardCharsets.UTF_8);
    server.createContext("/file.pdf", exchange -> {
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });

    server.start();

    Path target = targetDir.resolve("empty.pdf");
    Files.createFile(target);

    pdfDownloader.downloadIfMissing("http://localhost:" + port + "/file.pdf", target);

    assertEquals("fresh-content", Files.readString(target));
  }
}
