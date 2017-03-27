package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.admin.Session;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Handler for /admin/accounts which lists all empires in the database.
 */
public class AccountsHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    ArrayList<Account> accounts = DataStore.i.accounts().search();

    HashMap<Long, Empire> empires = new HashMap<>();
    for (Account account : accounts) {
      empires.put(account.empire_id, DataStore.i.empires().get(account.empire_id));
    }

    render("empires/accounts.html", ImmutableMap.<String, Object>builder()
        .put("accounts", accounts)
        .put("empires", empires)
        .build());
  }
}
