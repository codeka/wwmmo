package au.com.codeka.warworlds.server.handlers.admin;

import java.util.TreeMap;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.model.BackendUser;

public class AdminChatHandler extends AdminGenericHandler {
  @Override
  protected void get() throws RequestException {
    if (!isInRole(BackendUser.Role.ChatRead)) {
      render("admin/access-denied.html", null);
      return;
    }
    TreeMap<String, Object> data = new TreeMap<>();

    render("admin/chat/messages.html", data);
  }
}
