package au.com.codeka.warworlds.client.ctrl

import android.os.Build
import android.text.Html

fun fromHtml(str: String): CharSequence {
  return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
    @Suppress("DEPRECATION")
    Html.fromHtml(str)
  } else {
    Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY)
  }
}
