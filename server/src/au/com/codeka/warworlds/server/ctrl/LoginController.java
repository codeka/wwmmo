package au.com.codeka.warworlds.server.ctrl;

import java.security.SecureRandom;

import org.joda.time.DateTime;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseEmpire;
import au.com.codeka.common.protobuf.GenericError;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.BackendUser;

public class LoginController {
  private static final Log log = new Log("LoginController");

  private static final char[] SESSION_CHARS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

  // list of users that are allowed to impersonate others
  private static final String[] IMPERSONATE_USERS = {
      "dean@codeka.com.au", "warworldstest1@gmail.com", "warworldstest2@gmail.com",
      "warworldstest3@gmail.com", "warworldstest4@gmail.com"};

  /**
   * Generates a cookie, assuming we've just finished authenticating as the given email.
   */
  public String generateCookie(String emailAddr, boolean doAdminTest, String impersonateUser)
      throws RequestException {
    // generate a random string for the session cookie
    SecureRandom rand = new SecureRandom();
    StringBuilder cookie = new StringBuilder();
    for (int i = 0; i < 40; i++) {
      cookie.append(SESSION_CHARS[rand.nextInt(SESSION_CHARS.length)]);
    }

    boolean isAdmin = false;
    if (doAdminTest) {
      BackendUser backendUser = new AdminController().getBackendUser(emailAddr);
      if (backendUser != null) {
        isAdmin = true;
      }
    }

    if (impersonateUser != null) {
      boolean allowed = false;
      for (String impersonator : IMPERSONATE_USERS) {
        if (impersonator.equals(emailAddr)) {
          allowed = true;
        }
      }
      if (!allowed) {
        impersonateUser = null;
      }
    }

    createSession(cookie.toString(), emailAddr, impersonateUser, isAdmin);
    log.info("Authenticated: email=%s cookie=%s", emailAddr, cookie);
    return cookie.toString();
  }

  public Session createSession(String cookie, String emailAddr, @Nullable String impersonateUser,
      boolean isAdmin) throws RequestException {
    int empireID = 0;
    int allianceID = 0;
    boolean banned = false;
    String sql = "SELECT id, alliance_id, state FROM empires WHERE user_email = ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      if (impersonateUser != null) {
        stmt.setString(1, impersonateUser);
      } else {
        stmt.setString(1, emailAddr);
      }
      SqlResult res = stmt.select();
      if (res.next()) {
        empireID = res.getInt(1);
        if (res.getInt(2) == null) {
          allianceID = 0;
        } else {
          allianceID = res.getInt(2);
        }
        BaseEmpire.State state = BaseEmpire.State.fromNumber(res.getInt(3));
        if (state == BaseEmpire.State.BANNED) {
          banned = true;
        }
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }

    if (banned) {
      throw new RequestException(403, GenericError.ErrorCode.EmpireBanned,
          "You have been banned for misconduct.");
    }

    Session session = new Session(
        cookie.toString(), emailAddr, DateTime.now(), empireID, allianceID, isAdmin);
    new SessionController().saveSession(session);

    return session;
  }
}
