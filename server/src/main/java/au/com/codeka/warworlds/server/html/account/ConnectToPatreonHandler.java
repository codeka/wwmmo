package au.com.codeka.warworlds.server.html.account;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.PatreonBeginRequest;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.proto.PatreonInfo;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.store.EmpiresStore;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.google.common.io.BaseEncoding;
import com.patreon.PatreonAPI;
import com.patreon.PatreonOAuth;
import com.patreon.resources.Pledge;
import com.patreon.resources.User;

import java.io.IOException;
import java.util.List;

public class ConnectToPatreonHandler extends ProtobufRequestHandler {
  private static final Log log = new Log("ConnectToPatreonHandler");

  /**
   * Handler for where the client gets redirected to after successfully doing the oauth handshake.
   */
  @Override
  public void get() throws RequestException {
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
      PatreonBeginRequest req =
          PatreonBeginRequest.ADAPTER.decode(BaseEncoding.base64().decode(state));

      // Set up an empty PatreonInfo, that we'll then populate.
      PatreonInfo patreonInfo = new PatreonInfo.Builder()
          .empire_id(req.empire_id)
          .access_token(tokens.getAccessToken())
          .refresh_token(tokens.getRefreshToken())
          .token_expiry_time((long) tokens.getExpiresIn())
          .token_type(tokens.getTokenType())
          .token_scope(tokens.getScope())
          .build();
      DataStore.i.empires().savePatreonInfo(req.empire_id, patreonInfo);

      WatchableObject<Empire> empire = EmpireManager.i.getEmpire(req.empire_id);
      EmpireManager.i.refreshPatreonInfo(empire, patreonInfo);
    } catch (IOException e) {
      throw new RequestException(e);
    }

  }
}
