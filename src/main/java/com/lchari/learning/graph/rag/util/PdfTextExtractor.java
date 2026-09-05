package com.lchari.learning.graph.rag.util;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

public final class PdfTextExtractor {
  public static String extract(Path pdfPath) throws IOException {
    try(var document = Loader.loadPDF(pdfPath.toFile())) {
      return new PDFTextStripper().getText(document);
    }
  }
}
