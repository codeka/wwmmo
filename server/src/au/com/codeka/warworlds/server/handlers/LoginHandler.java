package au.com.codeka.warworlds.server.handlers;

import java.io.IOException;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.LoginController;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;

public class LoginHandler extends RequestHandler {
  private final Log log = new Log("LoginHandler");

  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = new GsonFactory();

  @Override
  protected void get() throws RequestException {
    // idTokens is the new way.
    String idToken = getRequest().getParameter("idToken");
    if (idToken != null) {
      // TODO: authenticate
    }

    // authToken is the old, soon to be unsupported way.
    String authToken = getRequest().getParameter("authToken");
    if (authToken == null || authToken.equals("null")) {
      throw new RequestException(400, "Bad login request.");
    }

    long startTime = System.currentTimeMillis();
    log.debug("Building OAuth credentials");
    GoogleCredential credential = new GoogleCredential();
    Oauth2 oauth2 = new Oauth2.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
        .setApplicationName("wwmmo")
        .build();

    try {
      log.debug("Executing OAuth request...");
      Tokeninfo tokenInfo = oauth2.tokeninfo()
          .setAccessToken(getRequest().getParameter("authToken")).execute();
      log.info("OAuth complete in %dms: %s",
          System.currentTimeMillis() - startTime, tokenInfo.getEmail());

      String emailAddr = tokenInfo.getEmail();
      String impersonateUser = getRequest().getParameter("impersonate");
      String cookie = new LoginController().generateCookie(
          emailAddr, tokenInfo.getAudience(), false, impersonateUser);

      getResponse().setContentType("text/plain");
      getResponse().getWriter().write(cookie);
    } catch (GoogleJsonResponseException e) {
      throw new RequestException(e.getStatusCode(), e.getStatusMessage(), e);
    } catch (IOException e) {
      throw new RequestException(e);
    }
  }
}
