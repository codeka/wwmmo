package au.com.codeka.warworlds.server.handlers.admin;

import java.util.HashMap;
import java.util.List;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;

public class AdminEmpireLoginsHandler extends AdminGenericHandler {
  private static final String PROD_CLIENT_ID =
      "1021675369049-85dehn126ib087kkc270k0lko6ahv2h7.apps.googleusercontent.com";
  private static final String DEV_CLIENT_ID =
      "1021675369049-kh3j8g9m8ugkrqamllddh3v0coss7gc8.apps.googleusercontent.com";

  @Override
  protected void get() throws RequestException {
    int empireID = Integer.parseInt(getUrlParameter("empireid"));

    List<Messages.EmpireLoginInfo> recentLogins = new EmpireController().getRecentLogins(empireID);

    HashMap<String, Object> data = new HashMap<>();
    data.put("logins", recentLogins);

    HashMap<String, String> clientIds = new HashMap<>();
    for (Messages.EmpireLoginInfo recentLogin : recentLogins) {
      String clientId = recentLogin.getClientId();
      if (!clientIds.containsKey(clientId)) {
        if (clientId.equals(PROD_CLIENT_ID)) {
          clientIds.put(clientId, "PROD");
        } else if (clientId.equals(DEV_CLIENT_ID)) {
          clientIds.put(clientId, "DEV");
        } else {
          clientIds.put(clientId, clientId);
        }
      }
    }
    data.put("clientIds", clientIds);

    render("admin/empire/ajax-recent-logins.html", data);
  }
}
