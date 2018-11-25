package au.com.codeka.warworlds.server.html;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

public class AuthenticatedRequestHandler extends ProtobufRequestHandler {
  @Nullable
  private WatchableObject<Empire> empire;

  /**
   * Gets the {@link Empire} that has authenticated this request.
   *
   * @throws RequestException if the request is not actually authenticated.
   */
  protected WatchableObject<Empire> getAuthenticatedEmpire() throws RequestException {
    if (empire == null) {
      String cookie = getRequest().getHeader("COOKIE");
      if (cookie == null) {
        throw new RequestException(400, "No COOKIE header found.");
      }

      Account acct = DataStore.i.accounts().get(cookie);
      if (acct == null) {
        throw new RequestException(400, "Invalid COOKIE.");
      }

      empire = EmpireManager.i.getEmpire(acct.empire_id);
    }

    return empire;
  }
}
