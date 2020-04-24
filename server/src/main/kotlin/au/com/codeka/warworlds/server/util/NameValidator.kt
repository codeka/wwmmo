package au.com.codeka.warworlds.server.util

/**
 * Helper class for validating the name of things.
 */
object NameValidator {
  fun validate(name: String, maxLength: Int): NameStatus {
    var name = name
    name = name.trim { it <= ' ' }
    if (name.length > maxLength) {
      return NameStatus(false, "Name too long.", null)
    }

    // Collapse multiple whitespace characters into one.
    name = name.replace("\\s+".toRegex(), " ")
    return NameStatus(true, null, name)
  }

  /** Same as [.validate], but instead of throwing, truncate.  */
  fun validateTruncate(name: String, maxLength: Int): String {
    var name = name
    name = name.trim { it <= ' ' }
    if (name.length > maxLength) {
      name = name.substring(0, maxLength)
    }

    // Collapse multiple whitespace characters into one.
    name = name.replace("\\s+".toRegex(), " ")
    return name
  }

  class NameStatus(var isValid: Boolean, var errorMsg: String?, var name: String?)
}