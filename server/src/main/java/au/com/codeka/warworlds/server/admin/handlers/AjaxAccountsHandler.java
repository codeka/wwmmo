package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.AccountManager;

/**
 * Handler for /admin/ajax/accounts, allowing various actions on accounts.
 */
public class AjaxAccountsHandler extends AjaxHandler {
  @Override
  public void post() throws RequestException {
    if (getRequest().getParameter("action").equals("resend")) {
      long empireId = Long.parseLong(getRequest().getParameter("id"));
      Account account = DataStore.i.accounts().getByEmpireId(empireId);
      if (account == null) {
        setResponseText("No account for empire #" + empireId);
        return;
      }
      AccountManager.i.sendVerificationEmail(account);
      setResponseText("success");
    }
  }
}
