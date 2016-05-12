package au.com.codeka.warworlds.client.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Helper class to fetch the contents of a URL.
 */
public class UrlFetcher {
  public static InputStream fetchStream(String urlString) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.addRequestProperty("User-Agent", "wwmmo/" + Version.string());
    if (conn.getResponseCode() == 200) {
      return conn.getInputStream();
    }
    return null;
  }
}
