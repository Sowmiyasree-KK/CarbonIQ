package com.carbuniq.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class PdfTextService {
  public String extractText(File pdfFile) throws Exception {
    try (PDDocument doc = PDDocument.load(pdfFile)) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(doc);
    }
  }
}