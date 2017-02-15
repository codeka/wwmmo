package au.com.codeka.warworlds.server.account;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.AccountAssociateRequest;
import au.com.codeka.warworlds.common.proto.AccountAssociateResponse;
import au.com.codeka.warworlds.server.ProtobufHttpServlet;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.util.TaskFuture;

/**
 * This servlet handles /accounts/associate, which is used to associate an account with a firebase
 * user.
 */
public class AccountAssociateServlet extends ProtobufHttpServlet {
  private static Log log = new Log("AccountAssociateServlet");

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    AccountAssociateRequest req = AccountAssociateRequest.ADAPTER.decode(request.getInputStream());

    FirebaseToken token;
    ListenableFuture<FirebaseToken> firebaseTokenFuture =
        new TaskFuture<>(FirebaseAuth.getInstance().verifyIdToken(req.token));

    Account account = DataStore.i.accounts().get(req.cookie);
    if (account == null) {
      log.warning("Could not associate account, no account for cookie: %s", req.cookie);
      response.setStatus(401);
      return;
    }

    try {
      token = Futures.get(firebaseTokenFuture, Exception.class);
    } catch (Exception e) {
      log.warning("Could not associate account, error fetching FirebaseToken.", e);
      response.setStatus(401);
      return;
    }

    AccountAssociateResponse.Builder resp = new AccountAssociateResponse.Builder();
    if (account.uid != null && account.uid.equals(token.getUid())) {
      log.info("Account is already associated with this firebase user, not doing anything");
      resp.status(AccountAssociateResponse.AccountAssociateStatus.SUCCESS);
    } else if (account.uid != null) {
      log.warning(
          "Account is already associated with another firebase, cannot associated with this one.");
      // Note: even if they're asked us to force this connection, we can't do it.
      resp.status(AccountAssociateResponse.AccountAssociateStatus.ACCOUNT_ALREADY_ASSOCIATED);
    } else {
      // The account is not associated with anybody, so we can associate it with this token. But
      // first, we must make sure this firebase user isn't already associated with something else.
      Account otherAccount = DataStore.i.accounts().getByUid(token.getUid());
      if (otherAccount != null && !req.force) {
        log.warning(
            "User '%s' already associated with empire #%d, cannot associate it with new account",
            token.getEmail(), otherAccount.empire_id);
        resp.status(AccountAssociateResponse.AccountAssociateStatus.TOKEN_ALREADY_ASSOCIATED);
      } else {
        if (otherAccount != null) {
          // they're forcing us.
          log.info("User '%s' already associated with empire #%d, but forcing new association.",
              token.getEmail(), otherAccount.empire_id);
        }

        account = account.newBuilder()
            .uid(token.getUid())
            .email(token.getEmail())
            .build();
        DataStore.i.accounts().put(req.cookie, account);
        resp.status(AccountAssociateResponse.AccountAssociateStatus.SUCCESS);
      }
    }

    writeProtobuf(response, resp.build());
  }
}
