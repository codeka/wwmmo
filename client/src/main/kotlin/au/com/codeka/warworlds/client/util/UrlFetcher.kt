package au.com.codeka.warworlds.client.util

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper class to fetch the contents of a URL.
 */
object UrlFetcher {
  @JvmStatic
  @Throws(IOException::class)
  fun fetchStream(urlString: String?): InputStream? {
    val url = URL(urlString)
    val conn = url.openConnection() as HttpURLConnection
    conn.addRequestProperty("User-Agent", "wwmmo/" + Version.string())
    return if (conn.responseCode == 200) {
      conn.inputStream
    } else null
  }
}