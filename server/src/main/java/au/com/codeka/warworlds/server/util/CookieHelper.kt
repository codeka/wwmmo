package au.com.codeka.warworlds.server.util

import java.security.SecureRandom

/**
 * Helper for working with cookies.
 */
object CookieHelper {
  private val COOKIE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()
  private const val COOKIE_LENGTH = 40
  fun generateCookie(): String {
    return generateCode(COOKIE_CHARS, COOKIE_LENGTH)
  }

  /** Generates a cookie, which is basically just a long-ish string of random bytes.  */
  private fun generateCode(alphabet: CharArray, length: Int): String {
    // generate a random string for the session cookie
    val rand = SecureRandom()
    val cookie = StringBuilder()
    for (i in 0 until length) {
      cookie.append(alphabet[rand.nextInt(alphabet.size)])
    }
    return cookie.toString()
  }
}