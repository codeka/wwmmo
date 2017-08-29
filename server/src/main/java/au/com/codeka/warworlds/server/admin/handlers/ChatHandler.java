package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import com.google.common.collect.ImmutableMap;

/**
 * Handles chat messages and stuff.
 */
public class ChatHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    render("chat/index.html", ImmutableMap.<String, Object>builder()
        .put("empires", DataStore.i.empires().search())
        .build());
  }
}
