package au.com.codeka.warworlds.client.net;

/**
 * This is an event that's posted to the event bus which reports on the state of the {@link Server}.
 */
public class ServerStateEvent {
  public enum ConnectionState {
    UNKNOWN,
    DISCONNECTED,
    CONNECTING,
    WAITING_FOR_HELLO,
    CONNECTED
  }

  private String url;
  private ConnectionState state;

  public ServerStateEvent(String url, ConnectionState state) {
    this.url = url;
    this.state = state;
  }

  public String getUrl() {
    return url;
  }

  public ConnectionState getState() {
    return state;
  }
}
