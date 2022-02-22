package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.Designs
import au.com.codeka.warworlds.common.sim.DesignHelper

/**
 * Handler for /admin/ajax/designs, returning the list of ship/building designs as a JSON file.
 */
class AjaxDesignsHandler : AdminHandler() {
  public override fun get() {
    setResponseJson(Designs(designs = DesignHelper.designs))
  }
}
