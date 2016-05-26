package au.com.codeka.warworlds.client.net;

import au.com.codeka.warworlds.client.util.GameSettings;

/**
 * Helper class for getting the current server URL.
 */
public class ServerUrl {
  public static String getUrl() {
    return GameSettings.i.getString(GameSettings.Key.SERVER);
  }

  public static String getWebSocketUrl() {
    String url = GameSettings.i.getString(GameSettings.Key.SERVER);
    return url.replace("http:", "ws:").replace("https:", "wss:") + "conn";
  }
}
