package au.com.codeka.warworlds.server.utils;

import au.com.codeka.warworlds.server.RequestException;

/**
 * Helper class for validating the name of things.
 */
public class NameValidator {
  public static String validate(String name, int maxLength) throws RequestException {
    name = name.trim();
    if (name.length() > maxLength) {
      throw new RequestException(400, "Name too long.");
    }

    // Collapse multiple whitespace characters into one.
    name = name.replaceAll("\\s+", " ");

    return name;
  }

  /** Same as {@link #validate}, but instead of throwing, truncate. */
  public static String validateTruncate(String name, int maxLength) {
    name = name.trim();
    if (name.length() > maxLength) {
      name = name.substring(0, maxLength);
    }

    // Collapse multiple whitespace characters into one.
    name = name.replaceAll("\\s+", " ");

    return name;
  }
}
