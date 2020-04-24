package au.com.codeka.warworlds.server.util

/**
 * Some helpers for working with email addresses.
 */
object EmailHelper {
  /**
   * Attempt some basic canonicalization of the given email address, to try to stop the most obvious
   * attempts at registering the same email address twice.
   *
   *
   * Gmail, for example, allows you to route email to different labels via a +xxx suffix on the
   * name. So for example, "foo@gmail" and "foo+bar@gmail.com" are actually the same account. In
   * this case, [.canonicalizeEmailAddress] will return "foo@gmail.com" as the
   * "canonical" name of "foo+bar@gmail.com" as well.
   */
  fun canonicalizeEmailAddress(emailAddr: String): String {
    // TODO: implement me
    return emailAddr
  }
}