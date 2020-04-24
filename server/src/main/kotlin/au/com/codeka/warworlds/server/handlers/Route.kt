package au.com.codeka.warworlds.server.handlers

import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest

class Route @JvmOverloads constructor(
    pattern: String,
    private val handlerClass: Class<out RequestHandler>,
    val extraOption: String? = null) {
  private val pattern: Pattern = Pattern.compile(pattern)

  /** @return The [Matcher] that matches the given request, or null if the request doesn't match. */
  fun matches(request: HttpServletRequest): Matcher? {
    var path = request.pathInfo
    if (path == null) {
      path = "/"
    }
    val matcher = pattern.matcher(path)
    return if (matcher.matches()) {
      matcher
    } else null
  }

  fun createRequestHandler(): RequestHandler {
    return handlerClass.newInstance()
  }
}