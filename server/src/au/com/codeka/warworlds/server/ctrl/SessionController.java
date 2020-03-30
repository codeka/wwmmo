package au.com.codeka.warworlds.server.ctrl;

import java.util.concurrent.TimeUnit;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.annotation.Nonnull;

public class SessionController {
  private static final Log log = new Log("SessionController");
  private static final LoadingCache<String, Session> SESSION_CACHE;
  private static final LoadingCache<String, Integer> EMPIRE_ID_CACHE;

  static {
    SESSION_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(24, TimeUnit.HOURS)
        .maximumSize(5000)
        .build(new CacheLoader<String, Session>() {
          @Override
          public Session load(@Nonnull String cookie) throws Exception {
            log.info("Loading session for cookie: %s", cookie);
            Session session = null;
            try (SqlStmt stmt = DB.prepare("SELECT * FROM sessions WHERE session_cookie=?")) {
              stmt.setString(1, cookie);
              SqlResult res = stmt.select();
              if (res.next()) {
                session = new Session(res);
              }
            }

            if (session == null && cookie.endsWith("_anon.war-worlds.com")) {
              // Anonymous cookies are OK, we always accept them.
              log.info("Session cookie indicates anonymous user: %s", cookie.replace('_', '@'));
              return new LoginController().createSession(
                  cookie, cookie.replace('_', '@'), "", null, false);
            }

            if (session != null) {
              new LoginController().updateSession(session);
              return session;
            }

            throw new RequestException(403, "Could not find session, session cookie: " + cookie);
          }
        });

    EMPIRE_ID_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(24, TimeUnit.HOURS)
        .maximumSize(10000)
        .build(new CacheLoader<String, Integer>() {
          @Override
          public Integer load(@Nonnull String email) throws Exception {
            try (SqlStmt stmt = DB.prepare("SELECT id FROM empires WHERE user_email = ?")) {
              stmt.setString(1, email);
              SqlResult res = stmt.select();
              if (res.next()) {
                return res.getInt(1);
              }
            }

            return null;
          }
        });
  }

  public void saveSession(Session session) throws RequestException {
    SESSION_CACHE.put(session.getCookie(), session);

    String sql = "DELETE FROM sessions WHERE session_cookie = ?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setString(1, session.getCookie());
      stmt.update();
    } catch (Exception e) {
      throw new RequestException(e);
    }

    sql = "INSERT INTO sessions (session_cookie, user_email, login_time, empire_id," +
        " alliance_id, is_admin, inline_notifications, client_id)" +
        " VALUES (?, ?, ?, ?, ?, ?, 0, ?)";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setString(1, session.getCookie());
      stmt.setString(2, session.getActualEmail());
      stmt.setDateTime(3, session.getLoginTime());
      if (session.getEmpireID() == 0) {
        stmt.setNull(4);
      } else {
        stmt.setInt(4, session.getEmpireID());
      }
      if (session.getAllianceID() == 0) {
        stmt.setNull(5);
      } else {
        stmt.setInt(5, session.getAllianceID());
      }
      stmt.setInt(6, session.isAdmin() ? 1 : 0);
      stmt.setString(7, session.getClientId());
      stmt.update();
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  public Session getSession(String cookie, String impersonateEmail) throws RequestException {
    try {
      Session session = SESSION_CACHE.get(cookie);

      if (impersonateEmail != null) {
        Integer empireID = EMPIRE_ID_CACHE.get(impersonateEmail);
        if (empireID != null) {
          session = session.impersonate(impersonateEmail, empireID);
        }
      }

      return session;
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }
}
