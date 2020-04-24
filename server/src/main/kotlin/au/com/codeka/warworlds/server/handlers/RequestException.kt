package au.com.codeka.warworlds.server.handlers

import java.util.*
import javax.servlet.http.HttpServletResponse

/** This exception is thrown when you want to pass an error back to the client. */
class RequestException : Exception {
  var errorCode = 0
    private set

  constructor(httpErrorCode: Int)
      : super(String.format(Locale.US, "HTTP Error: %d", httpErrorCode)) {
    errorCode = httpErrorCode
  }

  constructor(httpErrorCode: Int, message: String?)
      : super(String.format(message!!, httpErrorCode)) {
    errorCode = httpErrorCode
  }

  constructor(httpErrorCode: Int, message: String?, innerException: Throwable?)
      : super(String.format(message!!, httpErrorCode), innerException) {
    errorCode = httpErrorCode
  }

  constructor(innerException: Throwable)
      : super(getExceptionDescription(innerException), innerException) {
    val reqExc = findInnerException(innerException, RequestException::class.java)
    errorCode = reqExc?.errorCode ?: 500
  }

  fun populate(response: HttpServletResponse?) {
    response!!.status = errorCode
  }

  companion object {
    private const val serialVersionUID = 1L
    private fun getExceptionDescription(e: Throwable): String {
      val reqExc = findInnerException(e, RequestException::class.java)
      return if (reqExc != null) {
        "HTTP Error: " + reqExc.errorCode
      } else "Unknown Exception"
    }

    private fun <T : Exception?> findInnerException(e: Throwable, exceptionType: Class<T>): T? {
      var inner: Throwable? = e
      while (inner != null) {
        if (inner.javaClass == exceptionType) {
          @Suppress("UNCHECKED_CAST")
          return inner as T
        }
        inner = inner.cause
      }
      return null
    }
  }
}