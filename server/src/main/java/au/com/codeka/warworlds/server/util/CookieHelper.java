package au.com.codeka.warworlds.server.util;

import java.security.SecureRandom;

/**
 * Helper for working with cookies.
 */
public class CookieHelper {

  private static final char[] COOKIE_CHARS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
  private static final int COOKIE_LENGTH = 40;

  public static String generateCookie() {
    return generateCode(COOKIE_CHARS, COOKIE_LENGTH);
  }

  /** Generates a cookie, which is basically just a long-ish string of random bytes. */
  private static String generateCode(char[] alphabet, int length) {
    // generate a random string for the session cookie
    SecureRandom rand = new SecureRandom();
    StringBuilder cookie = new StringBuilder();
    for (int i = 0; i < length; i++) {
      cookie.append(alphabet[rand.nextInt(alphabet.length)]);
    }

    return cookie.toString();
  }

}
