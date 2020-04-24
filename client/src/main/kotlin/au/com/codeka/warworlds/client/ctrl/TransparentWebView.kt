package au.com.codeka.warworlds.client.ctrl

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import com.google.common.io.CharStreams
import java.io.InputStreamReader

/**
 * A [WebView] with a transparent background. It's a little tricky and the method for getting
 * a transparent background seems to be totally different depending on which version of the Android
 * API your device is.
 */
class TransparentWebView(context: Context?, attrs: AttributeSet?) : WebView(context, attrs) {
  override fun loadData(data: String, mimeType: String?, encoding: String?) {
    super.loadData(data, mimeType, encoding)
    setTransparent()
  }

  override fun loadDataWithBaseURL(
      baseUrl: String?,
      data: String,
      mimeType: String?,
      encoding: String?,
      historyUrl: String?) {
    super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    setTransparent()
  }

  /**
   * A helper that loads a template HTML (from your assets folder) and then replaces the string "%s"
   * in that template with the HTML you've supplied.
   */
  fun loadHtml(templateFileName: String?, html: String?) {
    var html = html
    val tmpl = getHtmlFile(context, templateFileName)
    html = String.format(tmpl, html)
    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  }

  private fun setTransparent() {
    setBackgroundColor(Color.TRANSPARENT)

    // This is required to make the background of the WebView actually transparent on Honeycomb+
    // (this API is only available on Honeycomb+ as well, so we need to call it via reflection...):
    // motdView.setLayerType(View.LAYER_TYPE_SOFTWARE, new Paint());
    try {
      val setLayerType = View::class.java.getMethod("setLayerType", Int::class.javaPrimitiveType, Paint::class.java)
      setLayerType?.invoke(this, 1, Paint())
    } catch (e: Exception) {
      // ignore if the method isn't supported on this platform...
    }
  }

  companion object {
    /** Loads a template HTML file from within your assets folder.  */
    @JvmStatic
    fun getHtmlFile(context: Context, fileName: String?): String {
      return try {
        val assetManager = context.assets
        val `is` = assetManager.open(fileName!!)
        CharStreams.toString(InputStreamReader(`is`, "utf-8"))
      } catch (e: Exception) {
        // any errors (shouldn't be...) and we'll return a "blank" template.
        ""
      }
    }
  }

  init {
    setTransparent()
  }
}