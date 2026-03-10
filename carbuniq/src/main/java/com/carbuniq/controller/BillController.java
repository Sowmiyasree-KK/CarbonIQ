package com.carbuniq.controller;

import com.carbuniq.model.BillReport;
import com.carbuniq.service.CarbonIQService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Controller
public class BillController {

  private final CarbonIQService carbonIQ;

  public BillController(
      @Value("${carbuniq.uploadsDir}") String uploadsDir,
      @Value("${carbuniq.reportsDir}") String reportsDir,
      @Value("${carbuniq.tesseractDataPath}") String tessDataPath,
      @Value("${carbuniq.emissionFactorKgPerKwh}") double emissionFactor,
      @Value("${carbuniq.solarCostPerKwInr}") double solarCostPerKw,
      @Value("${carbuniq.unitsPerKwPerMonth}") double unitsPerKwPerMonth
  ) {
    this.carbonIQ = new CarbonIQService(
        new File(uploadsDir),
        new File(reportsDir),
        tessDataPath,
        emissionFactor,
        solarCostPerKw,
        unitsPerKwPerMonth
    );
  }

  @GetMapping("/")
  public String uploadPage() {
    return "upload";
  }

  @PostMapping("/upload")
  public String uploadBill(@RequestParam("file") MultipartFile file,
                           @RequestParam(value = "hasAc", required = false) String hasAc,
                           @RequestParam(value = "hasGeyser", required = false) String hasGeyser) throws Exception {

    boolean ac = (hasAc != null);
    boolean geyser = (hasGeyser != null);

    File temp = File.createTempFile("bill-", "-" + file.getOriginalFilename());
    file.transferTo(temp);

    String billId = carbonIQ.processUpload(temp, file.getOriginalFilename(), ac, geyser);
    return "redirect:/report/" + billId;
  }

  @GetMapping("/report/{billId}")
  public String report(@PathVariable String billId, Model model) throws Exception {
    BillReport report = carbonIQ.loadReport(billId);
    model.addAttribute("report", report);
    return "report";
  }
}