package au.com.codeka.warworlds.server.handlers

import au.com.codeka.warworlds.common.Log
import com.google.common.hash.Hashing
import com.google.common.io.BaseEncoding
import com.google.gson.GsonBuilder
import com.squareup.wire.Message
import com.squareup.wire.WireTypeAdapterFactory
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.util.*
import java.util.regex.Matcher
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * This is the base class for the game's request handlers. It handles some common tasks such as
 * extracting protocol buffers from the request body, and so on.
 */
open class RequestHandler {
  private val log = Log("RequestHandler")
  protected lateinit var request: HttpServletRequest
    private set
  protected lateinit var response: HttpServletResponse
    private set
  private lateinit var routeMatcher: Matcher

  /** Gets the "extra" option that was passed in the route configuration.  */
  protected var extraOption: String? = null
    private set

  /** Set up this [RequestHandler], must be called before any other methods.  */
  fun setup(
      routeMatcher: Matcher,
      extraOption: String?,
      request: HttpServletRequest,
      response: HttpServletResponse) {
    this.routeMatcher = routeMatcher
    this.extraOption = extraOption
    this.request = request
    this.response = response
  }

  protected fun getUrlParameter(name: String): String? {
    return try {
      routeMatcher.group(name)
    } catch (e: IllegalArgumentException) {
      null
    }
  }

  open fun handle() {
    // start off with status 200, but the handler might change it
    response.status = 200
    try {
      if (!onBeforeHandle()) {
        return
      }
      when (request.method) {
        "GET" -> get()
        "POST" -> post()
        "PUT" -> put()
        "DELETE" -> delete()
        else -> throw RequestException(501)
      }
    } catch (e: RequestException) {
      handleException(e)
    } catch (e: Throwable) {
      log.error("Unexpected exception", e)
      handleException(RequestException(e))
    }
  }

  protected open fun handleException(e: RequestException) {
    log.error("Unhandled exception", e)
    throw e
  }

  /**
   * This is called before the get(), put(), etc methods but after the request is set up, ready to
   * go.
   *
   * @return true if we should continue processing the request, false if not. If you return false
   * then you should have set response headers, status code and so on already.
   */
  protected open fun onBeforeHandle(): Boolean {
    return true
  }

  protected open fun get() {
    throw RequestException(501)
  }

  protected fun put() {
    throw RequestException(501)
  }

  protected open fun post() {
    throw RequestException(501)
  }

  protected fun delete() {
    throw RequestException(501)
  }

  /**
   * Sets the required headers so that the client will know this response can be cached for the
   * given number of hours. The default response includes no caching headers.
   *
   * @param hours Time, in hours, to cache this response.
   * @param etag An optional value to include in the ETag header. This can be any string at all,
   * and we will hash and base-64 encode it for you.
   */
  protected fun setCacheTime(hours: Float, etag: String? = null) {
    response.setHeader(
        "Cache-Control",
        String.format(Locale.US, "private, max-age=%d", (hours * 3600).toInt()))
    if (etag != null) {
      val encodedETag =
          BaseEncoding.base64().encode(
              Hashing.sha256().hashString(etag, Charset.defaultCharset()).asBytes())
      response.setHeader("ETag", "\"${encodedETag}\"")
    }
  }

  protected fun setResponseText(text: String) {
    response.contentType = "text/plain"
    response.characterEncoding = "utf-8"
    try {
      response.writer.write(text)
    } catch (e: IOException) {
      // Ignore?
    }
  }

  protected fun setResponseJson(pb: Message<*, *>) {
    response.contentType = "application/json"
    response.characterEncoding = "utf-8"
    try {
      val writer = response.writer
      val gson = GsonBuilder()
          .registerTypeAdapterFactory(WireTypeAdapterFactory())
          .serializeSpecialFloatingPointValues()
          .disableHtmlEscaping()
          .create()
      var json = gson.toJson(pb)
      // serializeSpecialFloatingPointValues() will insert literal "Infinity" "-Infinity" and "NaN"
      // which is not valid JSON. We'll replace those with nulls in a kind-naive way.
      json = json
          .replace(":Infinity".toRegex(), ":null")
          .replace(":-Infinity".toRegex(), ":null")
          .replace(":NaN".toRegex(), ":null")
      writer.write(json)
      writer.flush()
    } catch (e: IOException) {
      // Ignore.
    }
  }

  protected fun setResponseGson(obj: Any) {
    response.contentType = "application/json"
    response.characterEncoding = "utf-8"
    try {
      val writer = response.writer
      val gson = GsonBuilder()
          .disableHtmlEscaping()
          .create()
      writer.write(gson.toJson(obj))
      writer.flush()
    } catch (e: IOException) {
      // Ignore.
    }
  }

  protected fun redirect(url: String) {
    response.status = 302
    response.addHeader("Location", url)
  }

  protected val requestUrl: String
    get() {
      val requestURI = URI(request.requestURL.toString())

      // TODO(dean): is hard-coding the https part for game.war-worlds.com the best way? no...
      return if (requestURI.host == "game.war-worlds.com") {
        "https://game.war-worlds.com" + requestURI.path
      } else {
        requestURI.toString()
      }
    }

  private fun <T> getRequestJson(protoType: Class<T>): T {
    val scanner = Scanner(request.inputStream, request.characterEncoding)
        .useDelimiter("\\A")
    val json = if (scanner.hasNext()) scanner.next() else ""
    return fromJson(json, protoType)
  }

  protected fun <T> fromJson(json: String, protoType: Class<T>): T {
    val gson = GsonBuilder()
        .registerTypeAdapterFactory(WireTypeAdapterFactory())
        .disableHtmlEscaping()
        .create()
    return gson.fromJson(json, protoType)
  }

  /** Get details about the given request as a string (for debugging).  */
  private fun getRequestDebugString(request: HttpServletRequest): String {
    return """
      ${request.requestURI}
      X-Real-IP: ${request.getHeader("X-Real-IP")}
      User-Agent: ${request.getHeader("User-Agent")}
      """.trimIndent()
  }
}
