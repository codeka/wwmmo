package au.com.codeka.warworlds.server.handlers.admin;

import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.List;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.handlers.LoginHandler;
import au.com.codeka.warworlds.server.model.Empire;

public class AdminEmpireLoginsHandler extends AdminGenericHandler {

  @Override
  protected void get() throws RequestException {
    EmpireController empireController = new EmpireController();

    List<Messages.EmpireLoginInfo> recentLogins;
    boolean isAjax;
    if (Strings.isNullOrEmpty(getUrlParameter("empireid"))) {
      recentLogins = empireController.getAllRecentLogins();
      isAjax = false;
    } else {
      int empireID = Integer.parseInt(getUrlParameter("empireid"));
      recentLogins = empireController.getRecentLogins(empireID);
      isAjax = true;
    }

    HashMap<String, Object> data = new HashMap<>();
    data.put("logins", recentLogins);

    HashMap<Integer, Empire> empires = new HashMap<>();
    HashMap<String, String> clientIds = new HashMap<>();
    for (Messages.EmpireLoginInfo recentLogin : recentLogins) {
      String clientId = recentLogin.getClientId();
      if (!clientIds.containsKey(clientId)) {
        if (clientId.equals(Configuration.PROD_CLIENT_ID)) {
          clientIds.put(clientId, "PROD");
        } else if (clientId.equals(Configuration.DEV_CLIENT_ID)) {
          clientIds.put(clientId, "DEV");
        } else {
          clientIds.put(clientId, clientId);
        }
      }

      int empireId = recentLogin.getEmpireId();
      if (!empires.containsKey(empireId)) {
        empires.put(empireId, empireController.getEmpire(empireId));
      }
    }
    data.put("clientIds", clientIds);
    data.put("empires", empires);

    if (isAjax) {
      render("admin/empire/ajax-recent-logins.html", data);
    } else {
      render("admin/empire/recent-logins.html", data);
    }
  }
}
