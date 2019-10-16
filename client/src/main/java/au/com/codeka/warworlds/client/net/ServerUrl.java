package au.com.codeka.warworlds.client.net;

import java.net.URI;
import java.net.URISyntaxException;

import au.com.codeka.warworlds.client.BuildConfig;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.Version;

/**
 * Helper class for getting the current server URL.
 */
public class ServerUrl {
  public static String getUrl() {
    return GameSettings.i.getString(GameSettings.Key.SERVER);
  }

  public static String getHost() {
    try {
      URI uri = new URI(getUrl());
      return uri.getHost();
    } catch (URISyntaxException e) {
      // Shouldn't happen.
      return null;
    }
  }

  public static String getUrl(String path) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return getUrl() + path;
  }
}
