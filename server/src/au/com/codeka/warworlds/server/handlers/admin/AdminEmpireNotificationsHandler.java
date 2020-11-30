package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.TreeMap;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.NotificationController;

public class AdminEmpireNotificationsHandler extends AdminHandler {
  private static final Log log = new Log("AdminEmpireNotificationsHandler");

  @Override
  protected void get() throws RequestException {
    if (!isAdmin()) {
      return;
    }
    int empireID = Integer.parseInt(getUrlParameter("empireid"));
    TreeMap<String, Object> data = new TreeMap<>();

    data.put("empireID", empireID);

    NotificationController notificationController = new NotificationController();
    ArrayList<String> tokens = notificationController.getFcmTokensForEmpire(empireID);
    data.put("tokens", tokens);

    render("admin/empire/ajax-notifications.html", data);
  }

  @Override
  protected void post() throws RequestException {
    if (!isAdmin()) {
      return;
    }

    int empireID = Integer.parseInt(getUrlParameter("empireid"));
    String msg = getRequest().getParameter("msg");


    new NotificationController().sendNotificationToEmpire(empireID, "debug-msg", msg);
  }
}
