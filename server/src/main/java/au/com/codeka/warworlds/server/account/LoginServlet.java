package au.com.codeka.warworlds.server.account;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import java.io.IOException;

import javax.annotation.Nullable;
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
import au.com.codeka.warworlds.server.util.TaskFuture;
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
    if (Strings.isNullOrEmpty(req.cookie)) {
      log.warning("No cookie in request, not connected.");
      response.setStatus(403);
      return;
    }

    ListenableFuture<FirebaseToken> firebaseTokenFuture = null;
    if (req.token != null) {
      firebaseTokenFuture = new TaskFuture<>(FirebaseAuth.getInstance().verifyIdToken(req.token));
    }

    log.info("Login request received, cookie=%s", req.cookie);
    Account account = DataStore.i.accounts().get(req.cookie);
    if (account == null) {
      log.warning("No account for cookie, not connecting: %s", req.cookie);
      response.setStatus(401);
      return;
    }

    WatchableObject<Empire> empire = EmpireManager.i.getEmpire(account.empire_id);
    if (empire == null) {
      log.warning("No empire with ID %d", account.empire_id);
      response.setStatus(404);
      return;
    }

    LoginResponse.LoginStatus loginStatus;
    if (firebaseTokenFuture != null) {
      try {
        FirebaseToken token = Futures.get(firebaseTokenFuture, Exception.class);
        loginStatus = verifyToken(account, token);
      } catch (Exception e) {
        log.warning("Error getting firebase token.", e);
        response.setStatus(500);
        return;
      }
    } else {
      loginStatus = verifyToken(account, null);
    }

    LoginResponse.Builder resp = new LoginResponse.Builder()
        .status(loginStatus);
    if (loginStatus == LoginResponse.LoginStatus.SUCCESS) {
      // Tell the server socket to expect a connection from this client.
      ServerSocketManager.i.addPendingConnection(account, empire, null /* encryptionKey */);

      resp.port(8081)
          .empire(empire.get());
    }
    writeProtobuf(response, resp.build());
  }

  /**
   * Checks that the given {@link FirebaseToken} can be used with the given {@link Account}.
   *
   * @param account The {@link Account}.
   * @param token The {@link FirebaseToken}, may be null if the user isn't logged in.
   *
   * @return A {@link LoginResponse.LoginStatus} that indicates whether the account and token are
   *         valid.
   */
  private LoginResponse.LoginStatus verifyToken(Account account, @Nullable FirebaseToken token) {
    if (account.uid != null) {
      if (token != null && account.uid.equals(token.getUid())) {
        log.debug("Account UID and token UID match, this is a valid token.");
        return LoginResponse.LoginStatus.SUCCESS;
      } else if (token == null) {
        log.warning("Account has a token, but there's no token supplied.");
        return LoginResponse.LoginStatus.ACCOUNT_NOT_ANONYMOUS;
      } else {
        log.warning("Account UID and token UID don't match (account email: %s, token email: %s)",
            account.email, token.getEmail());
        return LoginResponse.LoginStatus.ACCOUNT_TOKEN_MISMATCH;
      }
    } else {
      // If the account isn't associated with an token yet, that may be OK. However, the user will
      // need to explicitly associate the account.
      log.warning("Account doesn't have a UID, this is not a valid association.");
      return LoginResponse.LoginStatus.ACCOUNT_ANONYMOUS;
    }
  }
}
