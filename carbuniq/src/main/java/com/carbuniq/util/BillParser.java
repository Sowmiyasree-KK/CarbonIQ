package com.carbuniq.util;

import com.carbuniq.model.BillExtract;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BillParser {

  private static Double findNumberNearKeywords(String text, String[] keywords) {
    for (String k : keywords) {
      Pattern p = Pattern.compile(k + "[^0-9]{0,25}([0-9]+(\\.[0-9]+)?)",
          Pattern.CASE_INSENSITIVE);
      Matcher m = p.matcher(text);
      if (m.find()) return Double.parseDouble(m.group(1));
    }
    return null;
  }

  private static Double findRupeeAmountNearKeywords(String text, String[] keywords) {
    for (String k : keywords) {
      Pattern p = Pattern.compile(k + "[^0-9]{0,25}([0-9]{1,8}(\\.[0-9]{1,2})?)",
          Pattern.CASE_INSENSITIVE);
      Matcher m = p.matcher(text);
      if (m.find()) return Double.parseDouble(m.group(1));
    }
    return null;
  }

  private static String findBillingPeriod(String text) {
    Pattern p = Pattern.compile("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+20\\d\\d",
        Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(text);
    return m.find() ? m.group(0) : "Unknown";
  }

  public static BillExtract parse(String billId, String text, String source) {
    BillExtract b = new BillExtract();
    b.billId = billId;
    b.source = source;
    b.billingPeriod = findBillingPeriod(text);

    b.unitsKwh = findNumberNearKeywords(text, new String[]{
        "Units", "kWh", "Consumption", "Energy\\s*Consumed"
    });

    b.amount = findRupeeAmountNearKeywords(text, new String[]{
        "Amount\\s*Payable", "Net\\s*Payable", "Total\\s*Amount", "Grand\\s*Total", "Total"
    });

    if (b.unitsKwh != null && b.amount != null && b.unitsKwh > 0) {
      b.ratePerUnit = b.amount / b.unitsKwh;
    }
    return b;
  }
}