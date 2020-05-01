package au.com.codeka.warworlds.server.handlers;

import org.joda.time.DateTime;

import java.util.TreeMap;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.PatreonController;
import au.com.codeka.warworlds.server.model.PatreonInfo;
import au.com.codeka.warworlds.server.utils.PatreonApi;

/**
 * Handles requests for /empires/patreon, which is redirected to after a user links their empire
 * with Patreon.
 */
public class EmpiresPatreonHandler extends RenderingHandler {
  private static final Log log = new Log("EmpiresPatreonHandler");

  @Override
  protected void get() throws RequestException {
    String code = getRequest().getParameter("code");
    String state = getRequest().getParameter("state");

    log.info("Associated empire with patreon: [code=%s] [state=%s]", code, state);
    PatreonApi api = new PatreonApi();
    PatreonApi.TokensResponse tokens = api.getTokens(code);
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

    render("empires/patreon-thanks.html", new TreeMap<>());
  }
}
