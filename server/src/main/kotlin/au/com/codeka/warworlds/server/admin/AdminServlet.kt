package au.com.codeka.warworlds.server.admin

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.server.admin.handlers.*
import au.com.codeka.warworlds.server.handlers.HandlerServlet
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.handlers.Route
import com.google.common.collect.Lists
import java.util.regex.Matcher
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * The [AdminServlet] is the root of all requests for the admin backend.
 */
class AdminServlet : HandlerServlet(ROUTES) {
  override fun handle(
      matcher: Matcher,
      route: Route,
      request: HttpServletRequest,
      response: HttpServletResponse) {
    try {
      val session = getSession(request)
      val handler = route.createRequestHandler() as AdminHandler
      handler.setup(matcher, route.extraOption, session, request, response)
      handler.handle()
    } catch (e: RequestException) {
      e.populate(response)
    }
  }

  companion object {
    private val ROUTES = Lists.newArrayList(
        Route("/", DashboardHandler::class.java),
        Route("/login", AdminLoginHandler::class.java),
        Route("/sectors", SectorsHandler::class.java),
        Route("/starfield", StarfieldHandler::class.java),
        Route("/empires", EmpiresHandler::class.java),
        Route("/empires/(?<id>[0-9]+)", EmpireDetailsHandler::class.java),
        Route("/users", UsersHandler::class.java),
        Route("/users/create", UsersCreateHandler::class.java),
        Route("/chat", ChatHandler::class.java),
        Route("/debug/suspicious-events", DebugSuspiciousEventsHandler::class.java),
        Route("/debug/simulation-queue", DebugSimulationQueueHandler::class.java),
        Route("/debug/build-requests", DebugBuildRequestsHandler::class.java),
        Route("/debug/moving-fleets", DebugMovingFleetsHandler::class.java),
        Route("/ajax/accounts", AjaxAccountsHandler::class.java),
        Route("/ajax/chat", AjaxChatHandler::class.java),
        Route("/ajax/empire", AjaxEmpireHandler::class.java),
        Route("/ajax/designs", AjaxDesignsHandler::class.java),
        Route("/ajax/sectors", AjaxSectorsHandler::class.java),
        Route("/ajax/starfield", AjaxStarfieldHandler::class.java),
        Route("/ajax/users", AjaxUsersHandler::class.java),
        Route("/(?<path>.*)", AdminFileHandler::class.java)
    )

    private fun getSession(request: HttpServletRequest): Session? {
      if (request.cookies != null) {
        for (cookie in request.cookies) {
          if (cookie.name == "SESSION") {
            return SessionManager.i.getSession(cookie.value)
          }
        }
      }
      return null
    }
  }
}