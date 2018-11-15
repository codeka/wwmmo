package au.com.codeka.warworlds.client.net;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.LoginResponse;

/**
 * This is an event that's posted to the event bus which reports on the state of the {@link Server}.
 */
public class ServerStateEvent {
  public enum ConnectionState {
    UNKNOWN,
    DISCONNECTED,
    CONNECTING,
    WAITING_FOR_HELLO,
    CONNECTED,
    ERROR
  }

  private String url;
  private ConnectionState state;
  @Nullable private LoginResponse.LoginStatus loginStatus;

  public ServerStateEvent(
      String url,
      ConnectionState state,
      @Nullable LoginResponse.LoginStatus loginStatus) {
    this.url = url;
    this.state = state;
    this.loginStatus = loginStatus;
  }

  public String getUrl() {
    return url;
  }

  public ConnectionState getState() {
    return state;
  }

  /**
   * Gets the {@link LoginResponse.LoginStatus} from when we connected. This will only be
   * interesting when {@link #getState} is {@link ConnectionState#ERROR}.
   */
  public @Nullable LoginResponse.LoginStatus getLoginStatus() {
    return loginStatus;
  }
}
