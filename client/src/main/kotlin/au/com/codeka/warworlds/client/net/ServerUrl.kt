package au.com.codeka.warworlds.client.net

import au.com.codeka.warworlds.client.util.GameSettings
import java.net.URI
import java.net.URISyntaxException

/** Helper class for getting the current server URL. */
object ServerUrl {
  val url: String
    get() = GameSettings.getString(GameSettings.Key.SERVER)

  val host: String?
    get() = try {
      val uri = URI(url)
      uri.host
    } catch (e: URISyntaxException) {
      // Shouldn't happen.
      null
    }

  fun getUrl(path: String): String {
    var p = path
    if (p.startsWith("/")) {
      p = p.substring(1)
    }
    return url + p
  }
}
