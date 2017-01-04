package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

/** Handler for /admin/ajax/sectors requests. */
public class AjaxSectorsHandler extends AjaxHandler {
  @Override
  public void get() throws RequestException {
    switch (getRequest().getParameter("action")) {
      case "find-empty":
        handleFindEmptyRequest();
        break;
      default:
        throw new RequestException(400, "Unknown action: " + getRequest().getParameter("action"));
    }
  }

  private void handleFindEmptyRequest() {
    setResponseJson(DataStore.i.sectors().getEmptySector());
  }
}
