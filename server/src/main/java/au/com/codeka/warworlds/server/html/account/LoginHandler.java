package au.com.codeka.warworlds.server.html.account;

import com.google.common.base.Strings;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.LoginRequest;
import au.com.codeka.warworlds.common.proto.LoginResponse;
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.net.ServerSocketManager;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * This servlet is posted to in order to "log in". You'll get a pointer to the socket to connect
 * to for the actual main game connection.
 */
public class LoginHandler extends ProtobufRequestHandler {
  private final Log log = new Log("LoginHandler");

  @Override
  public void post() throws RequestException {
    LoginRequest req = readProtobuf(LoginRequest.class);
    if (Strings.isNullOrEmpty(req.cookie)) {
      log.warning("No cookie in request, not connected.");
      getResponse().setStatus(403);
      return;
    }

    log.info("Login request received, cookie=%s", req.cookie);
    Account account = DataStore.i.accounts().get(req.cookie);
    if (account == null) {
      log.warning("No account for cookie, not connecting: %s", req.cookie);
      getResponse().setStatus(401);
      return;
    }

    WatchableObject<Empire> empire = EmpireManager.i.getEmpire(account.empire_id);
    if (empire == null) {
      log.warning("No empire with ID %d", account.empire_id);
      getResponse().setStatus(404);
      return;
    }

    DataStore.i.stats().addLoginEvent(req, account);

    LoginResponse.Builder resp = new LoginResponse.Builder()
        .status(LoginResponse.LoginStatus.SUCCESS);
    // Tell the server socket to expect a connection from this client.
    ServerSocketManager.i.addPendingConnection(account, empire, null /* encryptionKey */);

    resp.port(8081).empire(empire.get());
    writeProtobuf(resp.build());
  }
}
