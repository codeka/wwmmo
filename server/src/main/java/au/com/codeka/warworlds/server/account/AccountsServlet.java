package au.com.codeka.warworlds.server.account;

import java.io.IOException;
import java.security.SecureRandom;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.NewAccountRequest;
import au.com.codeka.warworlds.common.proto.NewAccountResponse;
import au.com.codeka.warworlds.server.ProtobufHttpServlet;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/** Accounts servlet for creating new accounts on the server. */
public class AccountsServlet extends ProtobufHttpServlet {
  private final Log log = new Log("AccountsServlet");

  private static final char[] COOKIE_CHARS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
  private static final int COOKIE_LENGTH = 40;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    NewAccountRequest req = NewAccountRequest.ADAPTER.decode(request.getInputStream());
    log.info("Creating new account: %s", req.empire_name);

    // Reserve for ourselves an entry in the empire name map. If this fails, it meas there's already
    // an empire with the given name
//    if (!DataStore.i.uniqueEmpireNames().putIfNotExist(req.empire_name, -1L)) {
//      log.info("Could not create new account, empire name already exists: '%s'", req.empire_name);
//      writeProtobuf(response,
//          new NewAccountResponse.Builder()
//              .message("An empire with that name already exists.")
//              .build());
//      return;
//    }

    // Generate a cookie for the user to authenticate with in the future.
    String cookie = generateCookie();

    // Create the empire itself.
    WatchableObject<Empire> empire = EmpireManager.i.createEmpire(req.empire_name);
    if (empire == null) {
      // Some kind of unexpected error creating the empire.
      writeProtobuf(response,
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

    writeProtobuf(response,
        new NewAccountResponse.Builder()
            .cookie(cookie)
            .build());
  }

  /** Generates a cookie, which is basically just a long-ish string of random bytes. */
  private String generateCookie() {
    // generate a random string for the session cookie
    SecureRandom rand = new SecureRandom();
    StringBuilder cookie = new StringBuilder();
    for (int i = 0; i < COOKIE_LENGTH; i++) {
      cookie.append(COOKIE_CHARS[rand.nextInt(COOKIE_CHARS.length)]);
    }

    return cookie.toString();
  }
}
