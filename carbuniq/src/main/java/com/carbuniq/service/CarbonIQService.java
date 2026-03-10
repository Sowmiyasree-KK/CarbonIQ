package com.carbuniq.service;

import com.carbuniq.model.BillExtract;
import com.carbuniq.model.BillReport;
import com.carbuniq.model.SolarResult;
import com.carbuniq.util.BillParser;
import com.carbuniq.util.IdUtil;
import com.carbuniq.util.JsonStore;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class CarbonIQService {
  private final File uploadsDir;
  private final JsonStore store;
  private final PdfTextService pdfTextService;
  private final OcrService ocrService;

  private final double emissionFactor;
  private final double solarCostPerKw;
  private final double unitsPerKwPerMonth;

  public CarbonIQService(File uploadsDir,
                         File reportsDir,
                         String tessDataPath,
                         double emissionFactor,
                         double solarCostPerKw,
                         double unitsPerKwPerMonth) {
    this.uploadsDir = uploadsDir;
    if (!uploadsDir.exists()) uploadsDir.mkdirs();
    this.store = new JsonStore(reportsDir);
    this.pdfTextService = new PdfTextService();
    this.ocrService = new OcrService(tessDataPath);
    this.emissionFactor = emissionFactor;
    this.solarCostPerKw = solarCostPerKw;
    this.unitsPerKwPerMonth = unitsPerKwPerMonth;
  }

  public String processUpload(File file, String originalName, boolean hasAc, boolean hasGeyser) throws Exception {
    String billId = IdUtil.newId();
    String ext = guessExt(originalName);
    File saved = new File(uploadsDir, billId + ext);
    Files.copy(file.toPath(), saved.toPath());

    String text;
    String source;

    if (ext.equalsIgnoreCase(".pdf")) {
      text = pdfTextService.extractText(saved);
      if (text == null) text = "";
     
      source = "PDF_TEXT";
    } else {
      text = ocrService.doOcr(saved);
      source = "OCR_IMAGE";
    }

    BillExtract extract = BillParser.parse(billId, text, source);


    double units = extract.unitsKwh != null ? extract.unitsKwh : 0.0;
    double amount = extract.amount != null ? extract.amount : 0.0;
    double rate = (units > 0 && amount > 0) ? (amount / units) : 0.0;

   
    double carbonKg = units * emissionFactor;

    Map<String, Double> split = inferAppliances(units, hasAc, hasGeyser);

    
    List<String> rec = recommend(units, rate, hasAc, hasGeyser);


    SolarResult solar = solarEstimate(units, amount);

    BillReport report = new BillReport();
    report.extract = extract;
    report.carbonKg = round2(carbonKg);
    report.applianceSplitKwh = roundMap(split);
    report.recommendations = rec;
    report.solar = solar;

    report.predictedNextMonthBillInr = amount > 0 ? round2(amount * 1.03) : null;

    store.save(billId, report);
    return billId;
  }

  public BillReport loadReport(String billId) throws Exception {
    return store.load(billId);
  }

  private static String guessExt(String name) {
    if (name == null) return ".bin";
    String lower = name.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".pdf")) return ".pdf";
    if (lower.endsWith(".png")) return ".png";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return ".jpg";
    return ".bin";
  }

  private Map<String, Double> inferAppliances(double units, boolean hasAc, boolean hasGeyser) {
    Map<String, Double> map = new LinkedHashMap<>();
    if (units <= 0) {
      map.put("Unknown (no units extracted)", 0.0);
      return map;
    }

    double acPct = hasAc ? 0.40 : 0.10;
    double geyserPct = hasGeyser ? 0.15 : 0.05;
    double fridgePct = 0.12;
    double lightsPct = 0.13;
    double otherPct = Math.max(0, 1.0 - (acPct + geyserPct + fridgePct + lightsPct));

    map.put("AC", units * acPct);
    map.put("Geyser/Water Heater", units * geyserPct);
    map.put("Fridge", units * fridgePct);
    map.put("Lights/Fans", units * lightsPct);
    map.put("Others", units * otherPct);
    return map;
  }

  private List<String> recommend(double units, double rate, boolean hasAc, boolean hasGeyser) {
    List<String> r = new ArrayList<>();

    if (units <= 0) {
      r.add("Could not extract units from the bill. Try uploading a clearer image, or a text-based PDF.");
      r.add("As a fallback, you can extend the app to allow manual entry of units + amount.");
      return r;
    }

    if (hasAc && units > 200) {
      r.add("AC optimization: set 24°C + sleep mode; can reduce AC energy by ~10–20%.");
      r.add("Clean AC filters monthly to improve efficiency.");
    }
    if (hasGeyser) {
      r.add("Use geyser timer and avoid keeping it ON continuously to reduce wastage.");
    }
    r.add("Switch remaining bulbs to LED to reduce lighting energy significantly.");
    r.add("Reduce standby power: unplug idle chargers / use a power strip.");

    if (rate > 0 && units > 300) {
      r.add("High consumption detected: consider 5-star rated appliances for long-term savings.");
    }
    return r;
  }

  private SolarResult solarEstimate(double units, double monthlyBill) {
    SolarResult s = new SolarResult();

    if (units <= 0 || monthlyBill <= 0) {
      s.recommendedKw = null;
      s.estimatedCostInr = null;
      s.estimatedMonthlySavingsInr = null;
      s.paybackMonths = null;
      return s;
    }

    double sizeKw = Math.max(1.0, units / unitsPerKwPerMonth);
    double cost = sizeKw * solarCostPerKw;

   
    double monthlySavings = monthlyBill * 0.80;
    double paybackMonths = (monthlySavings > 0) ? (cost / monthlySavings) : 0;

    s.recommendedKw = round2(sizeKw);
    s.estimatedCostInr = round2(cost);
    s.estimatedMonthlySavingsInr = round2(monthlySavings);
    s.paybackMonths = round2(paybackMonths);
    return s;
  }

  private static double round2(double v) {
    return Math.round(v * 100.0) / 100.0;
  }

  private static Map<String, Double> roundMap(Map<String, Double> map) {
    Map<String, Double> out = new LinkedHashMap<>();
    for (var e : map.entrySet()) out.put(e.getKey(), round2(e.getValue()));
    return out;
  }
}