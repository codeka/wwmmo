package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;

import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Handler for /admin/empires which lists all empires in the database.
 */
public class EmpiresHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    render("empires/index.html", ImmutableMap.<String, Object>builder()
        .put("empires", DataStore.i.empires().search())
        .build());
  }
}
