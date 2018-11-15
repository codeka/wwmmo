package au.com.codeka.warworlds.server.admin.handlers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.AdminRole;
import au.com.codeka.warworlds.server.admin.Session;
import au.com.codeka.warworlds.server.admin.SessionManager;
import au.com.codeka.warworlds.server.handlers.RequestException;

public class AdminLoginHandler extends AdminHandler {
  private static final Log log = new Log("AdminLoginHandler");

  private static final String CLIENT_ID =
      "1021675369049-sumlr2cihs72j4okvfl8hl72keognhsa.apps.googleusercontent.com";

  /** We don't require any roles, because we're creating a session. */
  @Override
  protected Collection<AdminRole> getRequiredRoles() {
    return null;
  }

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
      JsonObject json = new JsonParser().parse(authResult).getAsJsonObject();
      String idToken = json.get("id_token").getAsString();

      TokenParser parser = new TokenParser(new String[] { CLIENT_ID }, CLIENT_ID);
      GoogleIdToken.Payload payload = parser.parse(idToken);
      if (payload == null) {
        throw new RequestException(500, parser.problem());
      }
      emailAddr = payload.getEmail();
      if (emailAddr == null) {
        String entries = null;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
          if (entries == null) {
            entries = "";
          } else {
            entries += ", ";
          }
          entries += String.format("%s = %s", entry.getKey(), entry.getValue());
        }
        entries += "\r\n" + authResult;
        throw new RequestException(500, "No email address: " + entries);
      }
    } catch (Exception e) {
      throw new RequestException(e);
    }

    Session session = SessionManager.i.authenticate(emailAddr);
    if (session == null) {
      // not a valid user
      redirect("/");
      return;
    }
    log.info("Got cookie: %s for %s", session.getCookie(), emailAddr);

    Cookie cookie = new Cookie("SESSION", session.getCookie());
    cookie.setHttpOnly(true);
    cookie.setPath("/admin");

    String continueUrl = getRequest().getParameter("continue");
    if (continueUrl == null) {
      continueUrl = "/admin";
    }

    log.info("Continuing to: %s", continueUrl);
    getResponse().addCookie(cookie);
    getResponse().setStatus(302);
    getResponse().setHeader("Location", continueUrl);
  }

  public class TokenParser {
    private final List<String> clientIDs;
    private final String audience;
    private final GoogleIdTokenVerifier verifier;
    private final JsonFactory jsonFactory;
    private String problem = "Verification failed. (Time-out?)";

    public TokenParser(String[] clientIDs, String audience) {
      this.clientIDs = Arrays.asList(clientIDs);
      this.audience = audience;
      jsonFactory = new GsonFactory();
      verifier = new GoogleIdTokenVerifier(new NetHttpTransport(), jsonFactory);
    }

    public GoogleIdToken.Payload parse(String tokenString) {
      GoogleIdToken.Payload payload = null;
      try {
        GoogleIdToken token = GoogleIdToken.parse(jsonFactory, tokenString);
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
