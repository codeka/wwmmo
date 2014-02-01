package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class SessionController {
    private static LoadingCache<String, Session> sSessionCache;
    private static LoadingCache<String, Integer> sEmpireIDCache;

    static {
        sSessionCache = CacheBuilder.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .maximumSize(5000)
                .build(new CacheLoader<String, Session>() {
                    @Override
                    public Session load(String cookie) throws Exception {
                        try (SqlStmt stmt = DB.prepare("SELECT * FROM sessions WHERE session_cookie=?")) {
                            stmt.setString(1, cookie);
                            ResultSet rs = stmt.select();
                            if (rs.next()) {
                                return new Session(rs);
                            }
                        }

                        throw new RequestException(403, "Could not find session, session cookie: "+cookie);
                    }
                });

        sEmpireIDCache = CacheBuilder.newBuilder()
                .expireAfterAccess(24, TimeUnit.HOURS)
                .maximumSize(10000)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String email) throws Exception {
                        try (SqlStmt stmt = DB.prepare("SELECT id FROM empires WHERE user_email = ?")) {
                            stmt.setString(1, email);
                            ResultSet rs = stmt.select();
                            if (rs.next()) {
                                return rs.getInt(1);
                            }
                        }

                        return null;
                    }
                });
    }

    public void saveSession(Session session) throws RequestException {
        sSessionCache.put(session.getCookie(), session);

        String sql = "DELETE FROM sessions WHERE session_cookie = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setString(1, session.getCookie());
            stmt.update();
        } catch (Exception e) {
            throw new RequestException(e);
        }

        sql = "INSERT INTO sessions (session_cookie, user_email, login_time, empire_id," +
              " alliance_id, is_admin, inline_notifications)" +
              " VALUES (?, ?, ?, ?, ?, ?, ?)";
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
            stmt.setInt(7, session.allowInlineNotifications() ? 1 : 0);
            stmt.update();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public Session getSession(String cookie, String impersonateEmail) throws RequestException {
        try {
            Session session = sSessionCache.get(cookie);

            if (impersonateEmail != null) {
                Integer empireID = sEmpireIDCache.get(impersonateEmail);
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
