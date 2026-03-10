package com.carbuniq.util;

import java.util.UUID;

public class IdUtil {
  public static String newId() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}