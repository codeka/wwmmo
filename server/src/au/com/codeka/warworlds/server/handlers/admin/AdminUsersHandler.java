package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.TreeMap;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.AdminController;
import au.com.codeka.warworlds.server.model.BackendUser;

import com.google.common.collect.Lists;

public class AdminUsersHandler extends AdminHandler {
  @Override
  protected void get() throws RequestException {
    if (!isAdmin()) {
      return;
    }
    TreeMap<String, Object> data = new TreeMap<String, Object>();
    data.put("users", new AdminController().getBackendUsers());
    data.put("roles", BackendUser.Role.values());
    render("admin/users.html", data);
  }

  @Override
  protected void post() throws RequestException {
    if (!isAdmin()) { // TODO: isInRole(SuperAdmin)
      return;
    }

    String email = getRequest().getParameter("email");
    ArrayList<BackendUser.Role> roles = Lists.newArrayList();
    for (BackendUser.Role role : BackendUser.Role.values()) {
      String value = getRequest().getParameter("role-" + role);
      if (value != null) {
        roles.add(role);
      }
    }

    new AdminController().createUser(new BackendUser(email, roles));

    redirect("/realms/" + getRealm() + "/admin/users");
  }
}
