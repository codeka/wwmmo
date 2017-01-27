package au.com.codeka.warworlds.server.admin.handlers;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;

import au.com.codeka.warworlds.common.proto.AdminUser;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.store.BaseStore;
import au.com.codeka.warworlds.server.store.DataStore;

/**
 * Handler for /admin/users, which allows you to view the users that have access to the backend.
 */
public class UsersHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
//    ArrayList<AdminUser> users = new ArrayList<>();
//    try (BaseStore.StoreCursor cursor = DataStore.i.adminUsers().search()) {
//      Pair<Long, AdminUser> pair;
//      while ((pair = cursor.next()) != null){
//        users.add(pair.second());
//      }
//    }

    render("users/index.html", ImmutableMap.<String, Object>builder()
        .put("users", null /*TODO users*/)
        .build());
  }
}
