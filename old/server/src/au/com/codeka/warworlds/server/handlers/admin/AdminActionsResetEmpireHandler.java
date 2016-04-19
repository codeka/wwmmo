package au.com.codeka.warworlds.server.handlers.admin;

import java.util.TreeMap;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;

/**
 * Handles the /admin/actions/move-star page.
 */
public class AdminActionsResetEmpireHandler extends AdminGenericHandler {

    @Override
    protected void post() throws RequestException {
        int empireID = Integer.parseInt(getRequest().getParameter("empire_id"));
        String reason = getRequest().getParameter("reason");

        TreeMap<String, Object> data = new TreeMap<String, Object>();
        data.put("complete", true);

        try {
            new EmpireController().resetEmpire(empireID, reason);
            data.put("success", true);
        } catch (RequestException e) {
            data.put("success", false);
            data.put("msg", e.getMessage());
        }

        render("admin/actions/reset-empire.html", data);
    }
}
