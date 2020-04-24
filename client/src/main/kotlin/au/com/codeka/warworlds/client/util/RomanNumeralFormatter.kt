package au.com.codeka.warworlds.client.util

/**
 * Simple helper class that formats numbers as roman numerals.
 */
object RomanNumeralFormatter {
  @JvmStatic
  fun format(n: Int): String {
    var n = n
    require(!(n < 0 || n >= 4000)) { "Cannot format numbers > 4000." }
    val sb = StringBuilder()
    while (n >= 1000) {
      sb.append("M")
      n -= 1000
    }
    while (n >= 900) {
      sb.append("CM")
      n -= 900
    }
    while (n >= 500) {
      sb.append("D")
      n -= 500
    }
    if (n >= 400) {
      sb.append("CD")
      n -= 400
    }
    while (n >= 100) {
      sb.append("C")
      n -= 100
    }
    if (n >= 90) {
      sb.append("XC")
      n -= 90
    }
    while (n >= 50) {
      sb.append("L")
      n -= 50
    }
    if (n >= 40) {
      sb.append("XL")
      n -= 40
    }
    while (n >= 10) {
      sb.append("X")
      n -= 10
    }
    if (n >= 9) {
      sb.append("IX")
      n -= 9
    }
    while (n >= 5) {
      sb.append("V")
      n -= 5
    }
    if (n >= 4) {
      sb.append("IV")
      n -= 4
    }
    while (n >= 1) {
      sb.append("I")
      n--
    }
    return sb.toString()
  }
}