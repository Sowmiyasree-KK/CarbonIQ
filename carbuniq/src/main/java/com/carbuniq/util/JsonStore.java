package com.carbuniq.util;

import com.carbuniq.model.BillReport;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class JsonStore {
  private final ObjectMapper mapper = new ObjectMapper();
  private final File reportsDir;

  public JsonStore(File reportsDir) {
    this.reportsDir = reportsDir;
    if (!reportsDir.exists()) reportsDir.mkdirs();
  }

  public void save(String billId, BillReport report) throws Exception {
    File f = new File(reportsDir, billId + ".json");
    mapper.writerWithDefaultPrettyPrinter().writeValue(f, report);
  }

  public BillReport load(String billId) throws Exception {
    File f = new File(reportsDir, billId + ".json");
    return mapper.readValue(f, BillReport.class);
  }
}