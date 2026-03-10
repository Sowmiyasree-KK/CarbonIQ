package com.carbuniq.service;

import net.sourceforge.tess4j.Tesseract;

import java.io.File;

public class OcrService {
  private final String tessDataPath;

  public OcrService(String tessDataPath) {
    this.tessDataPath = tessDataPath;
  }

  public String doOcr(File imageFile) throws Exception {
    Tesseract t = new Tesseract();
    t.setDatapath(tessDataPath);
    t.setLanguage("eng");
    return t.doOCR(imageFile);
  }
}