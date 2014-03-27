package au.com.codeka.warworlds.server.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.http.Cookie;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.LoginController;

public class LoginHandler extends RequestHandler {
    private final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    @Override
    protected void get() throws RequestException {
        boolean isLoadTest = false;
        String propValue = System.getProperty("au.com.codeka.warworlds.server.loadTest");
        if (propValue != null && propValue.equals("true")) {
            isLoadTest = true;
        }

        if (isLoadTest) {
            loadTestAuthenticate();
            return;
        }

        String authToken = getRequest().getParameter("authToken");

        // make a quick request to Google's Authorization endpoint to make sure the token they've
        // given us is actually valid (this'll also give us the actual email address)

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("https://www.googleapis.com/plus/v1/people/me?key=AIzaSyANXsZc4CaLMXDBJDClO9uAnzuYysQJ0zw");
        httpGet.addHeader("Authorization", "OAuth "+authToken);
        httpGet.addHeader("client_id", "1021675369049-cb56ts1l657ghi3ml2cg07t7c8t3dta9.apps.googleusercontent.com");
        httpGet.addHeader("client_secret", "6JQyk9rSsLHDJlenrsCWh4wv");

        JSONObject json;
        try {
            HttpResponse response = httpClient.execute(httpGet);
            InputStream ins = response.getEntity().getContent();
            String encoding = "utf-8";
            if (response.getEntity().getContentEncoding() != null) {
                encoding = response.getEntity().getContentEncoding().getValue();
            }
            InputStreamReader isr = new InputStreamReader(ins, encoding);

            if (response.getStatusLine().getStatusCode() != 200) {
                String responseBody = IOUtils.toString(isr);
                log.warn(String.format("%d %s\r\n%s", response.getStatusLine().getStatusCode(),
                        response.getStatusLine().getReasonPhrase(), responseBody));
                throw new RequestException(response.getStatusLine().getStatusCode(), "Error fetching user details.");
            }
            json = (JSONObject) JSONValue.parse(isr);
        } catch (IOException e) {
            throw new RequestException(e);
        }

        if (!json.containsKey("emails")) {
            throw new RequestException(500, "'emails' key expected.");
        }

        // eww, so much casting...
        JSONArray emailsArray = (JSONArray) json.get("emails");
        String emailAddr = (String) ((JSONObject) emailsArray.get(0)).get("value");
        String impersonateUser = getRequest().getParameter("impersonate");
        String cookie = new LoginController().generateCookie(emailAddr, false, impersonateUser);

        getResponse().setContentType("text/plain");
        try {
            getResponse().getWriter().write(cookie);
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    /**
     * Authentication for load tests is just based on trust. You pass in the email address you
     * want to use directly.
     */
    private void loadTestAuthenticate() throws RequestException {
        String emailAddr = getRequest().getParameter("email");
        String cookie = new LoginController().generateCookie(emailAddr, false, null);

        getResponse().addCookie(new Cookie("SESSION", cookie));
        getResponse().setStatus(200);
    }
}
