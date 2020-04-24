package au.com.codeka.warworlds.server.admin.handlers

/**
 * This handler is for /admin/sector, and allows us to debug some of the sector handling code.
 */
class SectorsHandler : AdminHandler() {
  public override fun get() {
    render("sectors/index.html", null)
  }
}