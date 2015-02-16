package au.com.codeka.warworlds.server.handlers.admin;

import java.util.TreeMap;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;

/**
 * Handles the /admin/actions/move-star page.
 */
public class AdminEmpireBanHandler extends AdminGenericHandler {

  @Override
  protected void post() throws RequestException {
    int empireID = Integer.parseInt(getRequest().getParameter("empire_id"));

    TreeMap<String, Object> data = new TreeMap<String, Object>();
    data.put("complete", true);

    try {
      new EmpireController().banEmpire(empireID);
      data.put("success", true);
    } catch (RequestException e) {
      data.put("success", false);
      data.put("msg", e.getMessage());
    }

    render("admin/empire/ban.html", data);
  }
}
