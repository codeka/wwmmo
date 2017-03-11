package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;

import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;

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
