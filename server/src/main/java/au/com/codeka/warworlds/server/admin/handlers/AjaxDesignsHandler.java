package au.com.codeka.warworlds.server.admin.handlers;

import au.com.codeka.warworlds.common.proto.Designs;
import au.com.codeka.warworlds.common.sim.DesignHelper;
import au.com.codeka.warworlds.server.handlers.RequestException;

/**
 * Handler for /admin/ajax/designs, returning the list of ship/building designs as a JSON file.
 */
public class AjaxDesignsHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    setResponseJson(new Designs.Builder().designs(DesignHelper.getDesigns()).build());
  }
}
