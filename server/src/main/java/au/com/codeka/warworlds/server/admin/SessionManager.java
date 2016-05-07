package au.com.codeka.warworlds.server.admin;

import org.joda.time.DateTime;

import java.security.SecureRandom;
import java.util.HashMap;

/**
 * Manages sessions in the admin backend. We keep current sessions live in memory, and don't bother
 * saving them to the database (if the server restarts, anybody using the backend will need to re-
 * authenticate).
 */
public class SessionManager {
  public static final SessionManager i = new SessionManager();

  private static final char[] COOKIE_CHARS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
  private static final int COOKIE_LENGTH = 40;

  private final HashMap<String, Session> sessions = new HashMap<>();

  public Session authenticate(String emailAddr) {
    // TODO: expire old sessions
    Session session = new Session(generateCookie(), emailAddr, DateTime.now());
    sessions.put(session.getCookie(), session);
    return session;
  }

  public Session getSession(String cookie) {
    // TODO: update session datetime
    return sessions.get(cookie);
  }

  /** Generates a cookie, which is basically just a long-ish string of random bytes. */
  private String generateCookie() {
    // generate a random string for the session cookie
    SecureRandom rand = new SecureRandom();
    StringBuilder cookie = new StringBuilder();
    for (int i = 0; i < COOKIE_LENGTH; i++) {
      cookie.append(COOKIE_CHARS[rand.nextInt(COOKIE_CHARS.length)]);
    }

    return cookie.toString();
  }
}
