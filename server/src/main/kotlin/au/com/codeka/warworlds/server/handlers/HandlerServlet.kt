package au.com.codeka.warworlds.server.handlers

import au.com.codeka.warworlds.common.Log
import java.util.regex.Matcher
import javax.servlet.GenericServlet
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Base [javax.servlet.Servlet] for working with handlers.
 */
open class HandlerServlet protected constructor(private val routes: List<Route>)
  : GenericServlet() {
  private val log = Log("HandlerServlet")
  override fun service(request: ServletRequest, response: ServletResponse) {
    for (route in routes) {
      val matcher = route.matches(request as HttpServletRequest)
      if (matcher != null) {
        handle(matcher, route, request, response as HttpServletResponse)
        return
      }
    }
    log.info(String.format(
        "Could not find handler for URL: %s",
        (request as HttpServletRequest).pathInfo))
    (response as HttpServletResponse).status = 404
  }

  protected open fun handle(
      matcher: Matcher,
      route: Route,
      request: HttpServletRequest,
      response: HttpServletResponse) {
    try {
      val handler = route.createRequestHandler()
      handler.setup(matcher, route.extraOption, request, response)
      handler.handle()
    } catch (e: RequestException) {
      e.populate(response)
    }
  }
}
