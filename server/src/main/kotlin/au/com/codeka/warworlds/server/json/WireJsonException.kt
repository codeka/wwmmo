package au.com.codeka.warworlds.server.json

class WireJsonException(msg: String, lineNo: Int = 0, e: Throwable? = null) : Exception(msg, e) {
  var lineNumber: Int = lineNo

  override fun toString(): String {
    return "Line: $lineNumber: ${super.toString()}"
  }
}
