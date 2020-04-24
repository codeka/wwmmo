package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.Designs
import au.com.codeka.warworlds.common.sim.DesignHelper
import au.com.codeka.warworlds.server.handlers.RequestException

/**
 * Handler for /admin/ajax/designs, returning the list of ship/building designs as a JSON file.
 */
class AjaxDesignsHandler : AdminHandler() {
  @Throws(RequestException::class)
  public override fun get() {
    setResponseJson(Designs.Builder().designs(DesignHelper.designs).build())
  }
}