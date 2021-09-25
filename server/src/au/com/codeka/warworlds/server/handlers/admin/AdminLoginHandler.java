package au.com.codeka.warworlds.server.handlers.admin;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.Cookie;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.AdminController;
import au.com.codeka.warworlds.server.ctrl.LoginController;
import au.com.codeka.warworlds.server.model.BackendUser;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AdminLoginHandler extends AdminHandler {
  private static final Log log = new Log("AdminLoginHandler");

  private static final String CLIENT_ID = "1021675369049-sumlr2cihs72j4okvfl8hl72keognhsa.apps.googleusercontent.com";

  @Override
  protected void get() throws RequestException {
    HashMap<String, Object> data = new HashMap<>();
    data.put("client_id", CLIENT_ID);
    render("login.html", data);
  }

  @Override
  protected void post() throws RequestException {
    String emailAddr;
    try {
      String authResult = getRequest().getParameter("auth-result");
      JsonObject json = JsonParser.parseString(authResult).getAsJsonObject();
      String idToken = json.get("id_token").getAsString();

      TokenParser parser = new TokenParser(new String[] { CLIENT_ID }, CLIENT_ID);
      GoogleIdToken.Payload payload = parser.parse(idToken);
      if (payload == null) {
        throw new RequestException(500, parser.problem());
      }
      emailAddr = payload.getEmail();
      if (emailAddr == null) {
        StringBuilder entries = new StringBuilder();
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
          if (entries.length() > 0) {
            entries.append(", ");
          }
          entries.append(entry.getKey());
          entries.append(" = ");
          entries.append(entry.getValue());
        }
        entries.append("\r\n");
        entries.append(authResult);
        throw new RequestException(500, "No email address: " + entries);
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }

    BackendUser backendUser = new AdminController().getBackendUser(emailAddr);
    if (backendUser == null) {
      render("admin/access-denied.html", new TreeMap<>());
      return;
    }
    new AdminController().recordLogin(backendUser);

    String cookieValue = new LoginController().generateCookie(emailAddr, "", true, null);
    log.info("Got cookie: %s", cookieValue);

    Cookie cookie = new Cookie("SESSION", cookieValue);
    cookie.setHttpOnly(true);
    cookie.setPath("/realms/" + getRealm());

    String continueUrl = getRequest().getParameter("continue");
    if (continueUrl == null) {
      continueUrl = "/realms/" + getRealm() + "/admin";
    }

    log.info("Continuing to: %s", continueUrl);
    getResponse().addCookie(cookie);
    getResponse().setStatus(302);
    getResponse().setHeader("Location", continueUrl);
  }

  public static class TokenParser {
    private final List<String> clientIDs;
    private final String audience;
    private final GoogleIdTokenVerifier verifier;
    private final JsonFactory jFactory;
    private String problem = "Verification failed. (Time-out?)";

    public TokenParser(String[] clientIDs, String audience) {
      this.clientIDs = Arrays.asList(clientIDs);
      this.audience = audience;
      NetHttpTransport transport = new NetHttpTransport();
      jFactory = new GsonFactory();
      verifier = new GoogleIdTokenVerifier(transport, jFactory);
    }

    public GoogleIdToken.Payload parse(String tokenString) {
      GoogleIdToken.Payload payload = null;
      try {
        GoogleIdToken token = GoogleIdToken.parse(jFactory, tokenString);
        if (verifier.verify(token)) {
          GoogleIdToken.Payload tempPayload = token.getPayload();
          if (!tempPayload.getAudience().equals(audience))
            problem = "Audience mismatch, " + audience + " != " + tempPayload.getAudience();
          else if (!clientIDs.contains(tempPayload.getAuthorizedParty()))
            problem = "Client ID mismatch";
          else
            payload = tempPayload;
        }
      } catch (GeneralSecurityException e) {
        problem = "Security issue: " + e.getLocalizedMessage();
      } catch (IOException e) {
        problem = "Network problem: " + e.getLocalizedMessage();
      }
      return payload;
    }

    public String problem() {
      return problem;
    }
  }
}
