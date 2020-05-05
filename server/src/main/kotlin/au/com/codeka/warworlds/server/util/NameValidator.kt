package au.com.codeka.warworlds.server.util

/**
 * Helper class for validating the name of things.
 */
object NameValidator {
  fun validate(name: String, maxLength: Int): NameStatus {
    var n = name
    n = n.trim { it <= ' ' }
    if (n.length > maxLength) {
      return NameStatus(false, "Name too long.", n)
    }

    // Collapse multiple whitespace characters into one.
    n = n.replace("\\s+".toRegex(), " ")
    return NameStatus(true, null, n)
  }

  /** Same as [.validate], but instead of throwing, truncate.  */
  fun validateTruncate(name: String, maxLength: Int): String {
    var n = name
    n = n.trim { it <= ' ' }
    if (n.length > maxLength) {
      n = n.substring(0, maxLength)
    }

    // Collapse multiple whitespace characters into one.
    n = n.replace("\\s+".toRegex(), " ")
    return n
  }

  class NameStatus(var isValid: Boolean, var errorMsg: String?, var name: String)
}