package au.com.codeka.warworlds.client.net;

import java.net.URI;
import java.net.URISyntaxException;

import au.com.codeka.warworlds.client.util.GameSettings;

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

  public static String getLoginUrl() {
    String url = GameSettings.i.getString(GameSettings.Key.SERVER);
    return url + "login";
  }
}
