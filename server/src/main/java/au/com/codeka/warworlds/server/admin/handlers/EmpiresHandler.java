package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashMap;

import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Handler for /admin/empires which lists all empires in the database. Actually, we search for all
 * accounts and then match them to the empire.
 */
public class EmpiresHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    ArrayList<Account> accounts = DataStore.i.accounts().search();

    HashMap<Long, Empire> empires = new HashMap<>();
    for (Account account : accounts) {
      empires.put(account.empire_id, DataStore.i.empires().get(account.empire_id));
    }

    render("empires/index.html", ImmutableMap.<String, Object>builder()
        .put("accounts", accounts)
        .put("empires", empires)
        .build());
  }
}
