package au.com.codeka.warworlds.server.account;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.LoginRequest;
import au.com.codeka.warworlds.common.proto.LoginResponse;
import au.com.codeka.warworlds.server.ProtobufHttpServlet;
import au.com.codeka.warworlds.server.net.ServerSocketManager;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * This servlet is posted to in order to "log in". You'll get a pointer to the socket to connect
 * to for the actual main game connection.
 */
public class LoginServlet extends ProtobufHttpServlet {
  private final Log log = new Log("LoginServlet");

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    LoginRequest req = LoginRequest.ADAPTER.decode(request.getInputStream());
    log.info("Login request received, cookie=%s", req.cookie);

    Account account = DataStore.i.accounts().get(req.cookie);
    if (account == null) {
      log.warning("No account for cookie, not connecting: %s", req.cookie);
      //TODO sendError(resp, 403, "Invalid cookie specified.");
      return;
    }
    WatchableObject<Empire> empire = EmpireManager.i.getEmpire(account.empire_id);

    // Tell the server socket to expect a connection from this client.
    ServerSocketManager.i.addPendingConnection(account, empire, null /* encryptionKey */);

    writeProtobuf(response,
        new LoginResponse.Builder()
            .port(8081)
            .empire(empire.get())
            .build());
  }
}
