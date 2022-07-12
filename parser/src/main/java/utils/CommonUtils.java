package utils;

import java.util.Locale;

public class CommonUtils {

  public static String replacePunctuation(String inputString) {
    return inputString.toLowerCase(Locale.ROOT).replaceAll("\\p{Punct}", " ").replaceAll("\r", " ");
  }
}
