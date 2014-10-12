package au.com.codeka.warworlds.server.handlers;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.http.Cookie;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Key;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.LoginController;

public class LoginHandler extends RequestHandler {
    private final Log log = new Log("RequestHandler");

    private static final String API_KEY = "AIzaSyANXsZc4CaLMXDBJDClO9uAnzuYysQJ0zw";
    private static final String CLIENT_ID = "1021675369049-cb56ts1l657ghi3ml2cg07t7c8t3dta9.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "6JQyk9rSsLHDJlenrsCWh4wv";

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new GsonFactory();
    private static final HttpRequestFactory REQUEST_FACTORY =
            HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                @Override
              public void initialize(HttpRequest request) {
                request.setParser(new JsonObjectParser(JSON_FACTORY));
              }
            });

    public static class PlusUrl extends GenericUrl {
        public PlusUrl(String encodedUrl) {
            super(encodedUrl);
        }

        @Key
        private final String key = API_KEY;

        public static PlusUrl me() {
            return new PlusUrl("https://www.googleapis.com/plus/v1/people/me");
        }
    }

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

        JsonObject json;
        try {
            HttpRequest request = REQUEST_FACTORY.buildGetRequest(PlusUrl.me());
            request.getHeaders().set("Authorization", Lists.newArrayList("OAuth " + authToken));
            request.getHeaders().set("client_id", CLIENT_ID);
            request.getHeaders().set("client_secret", CLIENT_SECRET);

            HttpResponse response = request.execute();
            String encoding = "utf-8";
            if (response.getContentCharset() != null) {
                encoding = response.getContentCharset().name();
            }
            InputStreamReader isr = new InputStreamReader(response.getContent(), encoding);

            if (response.getStatusCode() != 200) {
                String responseBody = CharStreams.toString(isr);
                log.warning("%d %s\r\n%s", response.getStatusCode(),
                        response.getStatusMessage(), responseBody);
                throw new RequestException(response.getStatusCode(), "Error fetching user details.");
            }
            json = new JsonParser().parse(isr).getAsJsonObject();
        } catch (IOException e) {
            throw new RequestException(e);
        }

        if (!json.has("emails")) {
            throw new RequestException(500, "'emails' key expected.");
        }

        JsonArray emailsArray = json.get("emails").getAsJsonArray();
        String emailAddr = emailsArray.get(0).getAsJsonObject().get("value").getAsString();
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
