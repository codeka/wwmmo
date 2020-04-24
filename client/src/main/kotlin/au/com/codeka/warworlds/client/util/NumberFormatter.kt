package au.com.codeka.warworlds.client.util

import java.text.DecimalFormat
import java.text.NumberFormat

/** Helper for formatting numbers in a nice way (with commas, essentially).  */
object NumberFormatter {
  private var format: NumberFormat? = null
  fun format(number: Int): String {
    return format!!.format(number.toLong())
  }

  fun format(number: Long): String {
    return format!!.format(number)
  }

  fun format(number: Double): String {
    return format!!.format(Math.ceil(number))
  }

  @JvmStatic
  fun format(number: Float): String {
    return format!!.format(Math.ceil(number.toDouble()))
  }

  init {
    format = NumberFormat.getInstance()
    (format as DecimalFormat?)!!.applyPattern("#,##0;-#,##0")
  }
}