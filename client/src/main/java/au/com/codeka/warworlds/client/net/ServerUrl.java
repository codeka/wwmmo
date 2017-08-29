package au.com.codeka.warworlds.client.net;

import au.com.codeka.warworlds.client.BuildConfig;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.Version;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Helper class for getting the current server URL.
 */
public class ServerUrl {
  public static String getUrl() {
    String serverUrl = GameSettings.i.getString(GameSettings.Key.SERVER);

    if (BuildConfig.DEBUG && Version.isEmulator()) {
      // In debug builds, on an emulator, we always want to connect to the host.
      try {
        URI uri = new URI(serverUrl);
        serverUrl = serverUrl.replace(uri.getHost(), "10.0.2.2");
      } catch (URISyntaxException e) {
        return serverUrl;
      }
    }
    return serverUrl;
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
