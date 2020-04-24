package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.proto.AdminRole
import au.com.codeka.warworlds.server.admin.Session
import au.com.codeka.warworlds.server.handlers.FileHandler
import au.com.codeka.warworlds.server.handlers.RequestException
import com.google.common.collect.Lists
import java.util.regex.Matcher
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/** Simple handler for handling static files (and 'templated' HTML files with no templated data).  */
class AdminFileHandler : AdminHandler() {
  private val fileHandler: FileHandler = FileHandler("data/admin/static/")

  /**
   * Gets a collection of roles, one of which the current user must be in to access this handler.
   */
  override val requiredRoles: Collection<AdminRole>?
    get() = Lists.newArrayList()

  override fun setup(
      routeMatcher: Matcher,
      extraOption: String?,
      session: Session?,
      request: HttpServletRequest,
      response: HttpServletResponse) {
    super.setup(routeMatcher, extraOption, session, request, response)
    fileHandler.setup(routeMatcher, extraOption, request, response)
  }

  override fun handle() {
    if (fileHandler.canHandle()) {
      fileHandler.handle()
      return
    }
    super.handle()
  }

  public override fun get() {
    var path = extraOption
    if (path == null) {
      path = ""
    }
    path += getUrlParameter("path")
    render(path, null)
  }
}