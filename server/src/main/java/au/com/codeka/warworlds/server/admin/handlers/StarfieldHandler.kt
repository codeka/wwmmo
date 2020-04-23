package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.server.handlers.RequestException

/**
 * This handler is for /admin/debug/starfield, and allows us to explore the starfield itself.
 */
class StarfieldHandler : AdminHandler() {
  public override fun get() {
    render("starfield/index.html", null)
  }
}