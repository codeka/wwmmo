package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.server.handlers.RequestException;

/**
 * This handler is for /admin/sector, and allows us to debug some of the sector handling code.
 */
public class SectorsHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    render("sectors/index.html", null);
  }
}
