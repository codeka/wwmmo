package au.com.codeka.warworlds.server.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.util.Locale;

import javax.servlet.http.Cookie;

import org.joda.time.DateTime;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.OpenIdAuth;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class LoginHandler extends RequestHandler {
    private final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private static char[] SESSION_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private static String[] ADMIN_USERS = {"dean@codeka.com.au"};

    @Override
    protected void get() throws RequestException {
        String authToken = getRequest().getParameter("authToken");
        if (authToken == null) {
            // it could be an OpenID authentication response.
            String openIdMode = getRequest().getParameter("openid.mode");
            if (openIdMode != null) {
                openIdAuthenticate();
                return;
            }
            throw new RequestException(400);
        }

        // make a quick request to Google's Authorization endpoint to make sure the token they've
        // given us is actually valid (this'll also give us the actual email address)
        URL url;
        try {
            url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token="+authToken);
        } catch (MalformedURLException e) {
            throw new RequestException(e); // should never happen
        }

        JSONObject json;
        try {
            URLConnection conn = url.openConnection();
            InputStream ins = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            if (encoding == null) {
                encoding = "utf-8";
            }
            InputStreamReader isr = new InputStreamReader(ins, encoding);
            json = (JSONObject) JSONValue.parse(isr);
        } catch (IOException e) {
            if (e.getMessage().indexOf("401") >= 0) {
                throw new RequestException(403, "Error requesting user info, token expired?", e);
            }
            throw new RequestException(e);
        }

        if (!json.containsKey("email")) {
            throw new RequestException(401);
        }
        String emailAddr = (String) json.get("email");
        String cookie = generateCookie(emailAddr, false);

        getResponse().setContentType("text/plain");
        try {
            getResponse().getWriter().write(cookie);
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    private void openIdAuthenticate() throws RequestException {
        String emailAddr = OpenIdAuth.getAuthenticatedEmail(getRequest());
        String cookie = generateCookie(emailAddr, true);

        getResponse().addCookie(new Cookie("SESSION", cookie));
        getResponse().setStatus(302);
        getResponse().addHeader("Location", "/admin"); // TODO: hard-coded?
    }

    /**
     * Generates a cookie, assuming we've just finihed authenticating as the given email.
     */
    private String generateCookie(String emailAddr, boolean doAdminTest) throws RequestException {
        // generate a random string for the session cookie
        SecureRandom rand = new SecureRandom();
        StringBuilder cookie = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            cookie.append(SESSION_CHARS[rand.nextInt(SESSION_CHARS.length)]);
        }

        boolean isAdmin = false;
        if (doAdminTest) {
            for (String adminUserEmail : ADMIN_USERS) {
                if (adminUserEmail.equals(emailAddr.toLowerCase())) {
                    isAdmin = true;
                }
            }
        }

        int empireID = 0;
        int allianceID = 0;
        try (SqlStmt stmt = DB.prepare("SELECT id, alliance_id FROM empires WHERE user_email = ?")) {
            stmt.setString(1, emailAddr);
            ResultSet rs = stmt.select();
            if (rs.next()) {
                empireID = rs.getInt(1);
                allianceID = rs.getInt(2);
            }
        } catch (Exception e) {
            throw new RequestException(e);
        }

        String sql = "INSERT INTO sessions (session_cookie, user_email, login_time, empire_id," +
                                          " alliance_id, is_admin)" +
                    " VALUES (?, ?, ?, ?, ?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setString(1, cookie.toString());
            stmt.setString(2, emailAddr);
            stmt.setDateTime(3, DateTime.now());
            if (empireID == 0) {
                stmt.setNull(4);
            } else {
                stmt.setInt(4, empireID);
            }
            if (allianceID == 0) {
                stmt.setNull(5);
            } else {
                stmt.setInt(5, allianceID);
            }
            stmt.setInt(6, isAdmin ? 1 : 0);
            stmt.update();
        } catch (Exception e) {
            throw new RequestException(e);
        }

        log.info(String.format(Locale.ENGLISH, "Authenticated: email=%s cookie=%s", emailAddr, cookie));
        return cookie.toString();
    }
}
