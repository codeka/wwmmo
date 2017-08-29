package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.common.proto.AdminUser;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import com.google.common.collect.ImmutableMap;
import java.util.List;

/**
 * Handler for /admin/users, which allows you to view the users that have access to the backend.
 */
public class UsersHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    List<AdminUser> users = DataStore.i.adminUsers().search();
    render("users/index.html", ImmutableMap.<String, Object>builder()
        .put("users", users)
        .build());
  }
}
