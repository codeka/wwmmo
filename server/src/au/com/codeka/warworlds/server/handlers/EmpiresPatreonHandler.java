package au.com.codeka.warworlds.server.handlers;

import com.patreon.PatreonOAuth;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.TreeMap;

import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.PatreonController;
import au.com.codeka.warworlds.server.model.PatreonInfo;

/**
 * Handles requests for /empires/patreon, which is redirected to after a user links their empire
 * with Patreon.
 */
public class EmpiresPatreonHandler extends RenderingHandler {
  @Override
  protected void get() throws RequestException {
    String code = getRequest().getParameter("code");
    String state = getRequest().getParameter("state");

    Configuration.PatreonConfig patreonConfig = Configuration.i.getPatreon();
    PatreonOAuth oauthClient =
        new PatreonOAuth(
            patreonConfig.getClientId(),
            patreonConfig.getClientSecret(),
            patreonConfig.getRedirectUri());
    try {
      PatreonOAuth.TokensResponse tokens = oauthClient.getTokens(code);
      long empireId = Long.parseLong(state);

      // Try to get the patreon info from the database, if the user's already connected.
      PatreonInfo patreonInfo = new PatreonController().getPatreonInfo(empireId);
      if (patreonInfo == null) {
        // Otherwise, set up a new one with the new info.
        patreonInfo = PatreonInfo.builder()
            .empireId(empireId)
            .build();
      }

      // And refresh the token stuff.
      patreonInfo = patreonInfo.newBuilder()
          .accessToken(tokens.getAccessToken())
          .refreshToken(tokens.getRefreshToken())
          .tokenExpiryTime(DateTime.now().plusSeconds(tokens.getExpiresIn()))
          .tokenType(tokens.getTokenType())
          .tokenScope(tokens.getScope())
          .build();

      new PatreonController().updatePatreonInfo(patreonInfo);
    } catch (IOException e) {
      throw new RequestException(e);
    }

    render("empires/patreon-thanks.html", new TreeMap<>());
  }
}
