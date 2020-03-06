package au.com.codeka.warworlds.server.handlers.admin;

import java.util.HashMap;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;

public class AdminEmpireLoginsHandler extends AdminGenericHandler {
  @Override
  protected void get() throws RequestException {
    int empireID = Integer.parseInt(getUrlParameter("empireid"));

    HashMap<String, Object> data = new HashMap<>();
    data.put("logins", new EmpireController().getRecentLogins(empireID));
    render("admin/empire/ajax-recent-logins.html", data);
  }
}
