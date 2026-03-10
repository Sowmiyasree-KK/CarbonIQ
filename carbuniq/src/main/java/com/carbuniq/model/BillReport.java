package com.carbuniq.model;

import java.util.List;
import java.util.Map;

public class BillReport {
  public BillExtract extract;
  public Double carbonKg;
  public Map<String, Double> applianceSplitKwh;
  public List<String> recommendations;
  public SolarResult solar;

 
  public Double predictedNextMonthBillInr;
}