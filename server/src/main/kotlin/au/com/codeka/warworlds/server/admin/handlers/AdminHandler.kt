package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.carrot.CarrotEngine
import au.com.codeka.carrot.Configuration
import au.com.codeka.carrot.bindings.MapBindings
import au.com.codeka.carrot.resource.FileResourceLocator
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.AdminRole
import au.com.codeka.warworlds.server.admin.Session
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.handlers.RequestHandler
import au.com.codeka.warworlds.server.store.DataStore
import com.google.common.base.Throwables
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.util.*
import java.util.regex.Matcher
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

open class AdminHandler : RequestHandler() {
  private var sessionNoError: Session? = null

  /**
   * Gets a collection of roles, one of which the current user must be in to access this handler.
   *
   * <p>Override this if you want to specify a different list of roles.
   */
  protected open val requiredRoles: Collection<AdminRole>?
    get() = Lists.newArrayList(AdminRole.ADMINISTRATOR)

  /** Set up this [RequestHandler], must be called before any other methods.  */
  open fun setup(
      routeMatcher: Matcher,
      extraOption: String?,
      session: Session?,
      request: HttpServletRequest,
      response: HttpServletResponse) {
    super.setup(routeMatcher, extraOption, request, response)
    sessionNoError = session
  }

  public override fun onBeforeHandle(): Boolean {
    val requiredRoles = requiredRoles
    if (requiredRoles != null) {
      val session = sessionNoError
      if (session == null) {
        authenticate()
        return false
      } else {
        var allowed = requiredRoles.isEmpty() // if requiredRoles is empty, they're OK.
        for (role in requiredRoles) {
          allowed = allowed || session.isInRole(role)
        }
        if (!allowed) {
          // you're not in a required role.
          log.warning("User '%s' is not in any required role: %s",
              session.email, requiredRoles)
          redirect("/admin")
          return false
        }
      }
    }
    return true
  }

  override fun handleException(e: RequestException) {
    try {
      render("exception.html", ImmutableMap.builder<String, Any>()
          .put("e", e)
          .put("stack_trace", Throwables.getStackTraceAsString(e))
          .build())
      e.populate(response)
    } catch (e2: Exception) {
      log.error("Error loading exception.html template.", e2)
      setResponseText(e2.toString())
    }
  }

  protected fun render(path: String, data: Map<String, Any>?) {
    val mutableData: MutableMap<String, Any> = when (data) {
      null -> TreeMap()
      is HashMap<String, Any> -> data
      is TreeMap<String, Any> -> data
      else -> TreeMap<String, Any>(data)
    }
    val session = sessionNoError
    if (session != null) {
      mutableData["session"] = session
    }
    mutableData["num_backend_users"] = DataStore.i.adminUsers().count()
    val contentType: String = when {
      path.endsWith(".css") -> "text/css"
      path.endsWith(".js") -> "text/javascript"
      path.endsWith(".html") -> "text/html"
      else -> "text/plain"
    }
    response.contentType = contentType
    response.setHeader("Content-Type", contentType)
    response.writer.write(CARROT.process(path, MapBindings(mutableData)))
  }

  protected fun write(text: String) {
    response.contentType = "text/plain"
    response.setHeader("Content-Type", "text/plain; charset=utf-8")
    try {
      response.writer.write(text)
    } catch (e: IOException) {
      log.error("Error writing output!", e)
    }
  }

  protected fun getSession(): Session {
    if (sessionNoError == null) {
      throw RequestException(403)
    }
    return sessionNoError!!
  }

  /** Checks whether the current user is in the given role. */
  protected fun isInRole(role: AdminRole): Boolean {
    return if (sessionNoError == null) {
      false
    } else sessionNoError!!.isInRole(role)
  }

  private fun authenticate() {
    val requestUrl: URI
    requestUrl = try {
      URI(super.requestUrl)
    } catch (e: URISyntaxException) {
      throw RuntimeException(e)
    }
    val finalUrl = requestUrl.path
    var redirectUrl = requestUrl.resolve("/admin/login").toString()
    try {
      redirectUrl += "?continue=" + URLEncoder.encode(finalUrl, "utf-8")
    } catch (e: UnsupportedEncodingException) {
      // should never happen
    }
    redirect(redirectUrl)
  }

  private class SessionHelper {
    fun isInRole(session: Session, roleName: String?): Boolean {
      val role = AdminRole.valueOf(roleName!!)
      return session.isInRole(role)
    }
  }

  private class StringHelper {
    // Used by template engine.
    fun trunc(s: String, maxLength: Int): String {
      return if (s.length > maxLength - 3) {
        s.substring(0, maxLength - 3) + "..."
      } else s
    }
  }

  private class GsonHelper {
    // Used by template engine.
    fun encode(obj: Any?): String {
      return Gson().toJson(obj)
    }
  }

  companion object {
    private val log = Log("AdminHandler")
    private val CARROT = CarrotEngine(
        Configuration.Builder()
            .setResourceLocator(
                FileResourceLocator.Builder(File("data/admin/tmpl").absolutePath))
            .setLogger { level: Int, msg: String ->
              val message = msg.replace("%", "%%")
              when (level) {
                Configuration.Logger.LEVEL_DEBUG -> log.debug("CARROT: %s", message)
                Configuration.Logger.LEVEL_INFO -> log.info("CARROT: %s", message)
                Configuration.Logger.LEVEL_WARNING -> log.warning("CARROT: %s", message)
                else -> log.error("(level: %d): CARROT: %s", level, message)
              }
            }
            .build(),
        MapBindings.Builder()
            .set("Session", SessionHelper())
            .set("String", StringHelper())
            .set("Gson", GsonHelper()))
  }
}