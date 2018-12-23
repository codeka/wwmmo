package au.com.codeka.warworlds.server.util;

import au.com.codeka.warworlds.server.handlers.RequestException;

import javax.annotation.Nullable;

/**
 * Helper class for validating the name of things.
 */
public class NameValidator {
  public static class NameStatus {
    public boolean isValid;
    @Nullable public String errorMsg;
    @Nullable public String name;

    private NameStatus(boolean isValid, @Nullable String errorMsg, @Nullable String name) {
      this.isValid = isValid;
      this.errorMsg = errorMsg;
      this.name = name;
    }
  }

  public static NameStatus validate(String name, int maxLength) {
    name = name.trim();
    if (name.length() > maxLength) {
      return new NameStatus(false, "Name too long.", null);
    }

    // Collapse multiple whitespace characters into one.
    name = name.replaceAll("\\s+", " ");

    return new NameStatus(true, null, name);
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
