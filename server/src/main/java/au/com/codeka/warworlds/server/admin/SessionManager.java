package au.com.codeka.warworlds.server.admin;

import org.joda.time.DateTime;

import java.security.SecureRandom;
import java.util.HashMap;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.common.proto.AdminUser;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Manages sessions in the admin backend. We keep current sessions live in memory, and don't bother
 * saving them to the database (if the server restarts, anybody using the backend will need to re-
 * authenticate).
 */
public class SessionManager {
  private static final Log log = new Log("SessionManager");
  public static final SessionManager i = new SessionManager();

  private static final char[] COOKIE_CHARS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
  private static final int COOKIE_LENGTH = 40;

  private final HashMap<String, Session> sessions = new HashMap<>();

  public Session authenticate(String emailAddr) {
    // TODO: expire old sessions
    AdminUser user;
    if (DataStore.i.adminUsers().count() == 0) {
      // If you don't have any users, then everybody who authenticates is an administrator.
      user = new AdminUser.Builder()
          .email_addr(emailAddr)
          .role(AdminRole.ADMINISTRATOR)
          .build();
    } else {
      user = DataStore.i.adminUsers().get(emailAddr);
      if (user == null) {
        log.warning("User '%s' is not a valid admin user.", emailAddr);
        return null;
      }
    }

    Session session = new Session(generateCookie(), user, DateTime.now());
    sessions.put(session.getCookie(), session);
    return session;
  }

  public Session getSession(String cookie) {
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
