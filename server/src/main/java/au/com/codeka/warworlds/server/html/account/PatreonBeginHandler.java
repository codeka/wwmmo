package au.com.codeka.warworlds.server.html.account;

import com.google.common.io.BaseEncoding;

import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.PatreonBeginRequest;
import au.com.codeka.warworlds.common.proto.PatreonBeginResponse;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.html.AuthenticatedRequestHandler;
import au.com.codeka.warworlds.server.world.WatchableObject;

public class PatreonBeginHandler extends AuthenticatedRequestHandler {
  @Override
  public void post() throws RequestException {
    PatreonBeginRequest req = readProtobuf(PatreonBeginRequest.class);
    WatchableObject<Empire> empire = getAuthenticatedEmpire();
    if (!req.empire_id.equals(empire.get().id)) {
      throw new RequestException(400, "Empire ID does not match.");
    }

    // We currently don't save this as a "begun" patreon request.. don't really care.

    Configuration.PatreonConfig config = Configuration.i.getPatreon();
    writeProtobuf(new PatreonBeginResponse.Builder()
        .client_id(config.getClientId())
        .redirect_uri(config.getRedirectUri())
        .state(BaseEncoding.base64().encode(req.encode()))
        .build());
  }
}
