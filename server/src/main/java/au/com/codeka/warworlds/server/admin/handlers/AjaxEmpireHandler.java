package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.handlers.RequestException;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/** Handler for /admin/ajax/empires/&lt;id&gt; */
public class AjaxEmpireHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    if (getRequest().getParameter("id") != null) {
      long empireId = Long.parseLong(getRequest().getParameter("id"));
      WatchableObject<Empire> empire = EmpireManager.i.getEmpire(empireId);
      if (empire == null) {
        throw new RequestException(404);
      }
      setResponseJson(empire.get());
    }
  }
}
