package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Handler for /admin/ajax/users which lets us modify some backend user stuff.
 */
public class AjaxUsersHandler extends AjaxHandler {
  @Override
  public void post() throws RequestException {
    switch (getRequest().getParameter("action")) {
      case "delete":
        String emailAddr = getRequest().getParameter("email_addr");
        handleDeleteRequest(emailAddr);
        break;
      default:
        throw new RequestException(400, "Unknown action: " + getRequest().getParameter("action"));
    }
  }

  private void handleDeleteRequest(String emailAddr) {
    DataStore.i.adminUsers().delete(emailAddr);
  }
}
