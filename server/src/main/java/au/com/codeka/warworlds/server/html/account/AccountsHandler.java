package au.com.codeka.warworlds.server.html.account;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.NewAccountRequest;
import au.com.codeka.warworlds.common.proto.NewAccountResponse;
import au.com.codeka.warworlds.server.handlers.ProtobufRequestHandler;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.util.CookieHelper;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/** Accounts servlet for creating new accounts on the server. */
public class AccountsHandler extends ProtobufRequestHandler {
  private final Log log = new Log("EmpiresHandler");

  @Override
  public void post() throws RequestException {
    NewAccountRequest req = readProtobuf(NewAccountRequest.class);
    log.info("Creating new account: %s", req.empire_name);

    // Generate a cookie for the user to authenticate with in the future.
    String cookie = CookieHelper.generateCookie();

    // Create the empire itself.
    WatchableObject<Empire> empire = EmpireManager.i.createEmpire(req.empire_name);
    if (empire == null) {
      // Some kind of unexpected error creating the empire.
      writeProtobuf(
          new NewAccountResponse.Builder()
              .message("An error occurred while creating your empire, please try again.")
              .build());
      return;
    }

    // Make a new account with all the details.
    Account acct = new Account.Builder()
        .empire_id(empire.get().id)
        .build();
    DataStore.i.accounts().put(cookie, acct);

    writeProtobuf(
        new NewAccountResponse.Builder()
            .cookie(cookie)
            .build());
  }

}
