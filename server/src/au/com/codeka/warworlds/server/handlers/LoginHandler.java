package au.com.codeka.warworlds.server.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.Locale;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class LoginHandler extends RequestHandler {
    private final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private static char[] SESSION_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    @Override
    protected void get() throws RequestException {
        String authToken = getRequest().getParameter("authToken");
        if (authToken == null) {
            throw new RequestException(400);
        }

        // make a quick request to Google's Authorization endpoint to make sure the token they've
        // given us is actually valid (this'll also give us the actual email address)
        URL url;
        try {
            url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token="+authToken);
        } catch (MalformedURLException e) {
            throw new RequestException(500, e); // should never happen
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
            char[] content = new char[4000];
            int numRead = 0;
            while (true) {
                int chars = isr.read(content, numRead, content.length - numRead);
                if (chars < 0) {
                    break;
                }
                numRead += chars;
            }

            json = new JSONObject(new String(content));
        } catch (IOException e) {
            throw new RequestException(500, e);
        }

        if (!json.has("email")) {
            throw new RequestException(401);
        }
        String emailAddr = json.getString("email");

        // generate a random string for the session cookie
        SecureRandom rand = new SecureRandom();
        StringBuilder cookie = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            cookie.append(SESSION_CHARS[rand.nextInt(SESSION_CHARS.length)]);
        }

        try (SqlStmt sql = DB.prepare("INSERT INTO sessions (session_cookie, user_email, login_time) VALUES (?, ?, ?)")) {
            sql.setString(1, cookie.toString());
            sql.setString(2, emailAddr);
            sql.setDateTime(3, DateTime.now());
            sql.update();
        } catch (Exception e) {
            throw new RequestException(500, e);
        }

        log.info(String.format(Locale.ENGLISH, "Authenticated: email=%s cookie=%s", emailAddr, cookie));

        getResponse().setContentType("text/plain");
        try {
            getResponse().getWriter().write(cookie.toString());
        } catch (IOException e) {
            throw new RequestException(500, e);
        }
    }
}
