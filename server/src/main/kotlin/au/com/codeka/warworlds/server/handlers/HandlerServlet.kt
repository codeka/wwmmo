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
    val httpRequest = request as HttpServletRequest
    val httpResponse = response as HttpServletResponse

    for (route in routes) {
      val matcher = route.matches(httpRequest)
      if (matcher != null) {
        handle(matcher, route, httpRequest, httpResponse)
        return
      }
    }

    log.info("Could not find handler for URL: ${httpRequest.pathInfo}")
    httpResponse.status = 404
  }

  protected open fun handle(
      matcher: Matcher, route: Route, request: HttpServletRequest, response: HttpServletResponse) {
    try {
      val handler = route.createRequestHandler()
      handler.setup(matcher, route.extraOption, request, response)
      handler.handle()
    } catch (e: RequestException) {
      e.populate(response)
    }
  }
}
