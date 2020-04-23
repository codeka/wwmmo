package au.com.codeka.warworlds.server.handlers

import com.google.common.base.Preconditions
import com.google.firebase.database.annotations.Nullable
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest

class Route @JvmOverloads constructor(
    pattern: String,
    handlerClass: Class<out RequestHandler>,
    extraOption: String? = null) {
  private val pattern: Pattern
  private val handlerClass: Class<out RequestHandler>
  val extraOption: String?

  /** @return The [Matcher] that matches the given request, or null if the request doesn't match. */
  @Nullable
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

  @Throws(RequestException::class)
  fun createRequestHandler(): RequestHandler {
    return try {
      handlerClass.newInstance()
    } catch (e: InstantiationException) {
      throw RequestException(e)
    } catch (e: IllegalAccessException) {
      throw RequestException(e)
    }
  }

  init {
    this.pattern = Pattern.compile(Preconditions.checkNotNull(pattern))
    this.handlerClass = handlerClass
    this.extraOption = extraOption
  }
}