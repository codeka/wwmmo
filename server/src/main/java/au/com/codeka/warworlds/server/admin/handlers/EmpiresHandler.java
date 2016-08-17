package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.server.admin.RequestException;

/**
 * Handler for /admin/empires which lists all empires in the database.
 */
public class EmpiresHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    render("empires/index.html", null);
  }
}
