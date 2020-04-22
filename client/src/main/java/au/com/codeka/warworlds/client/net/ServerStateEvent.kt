package au.com.codeka.warworlds.client.net

import au.com.codeka.warworlds.common.proto.LoginResponse
import au.com.codeka.warworlds.common.proto.LoginResponse.LoginStatus

/**
 * This is an event that's posted to the event bus which reports on the state of the [Server].
 */
class ServerStateEvent(
    val url: String?,
    val state: ConnectionState,
    val loginStatus: LoginStatus?) {
  enum class ConnectionState {
    UNKNOWN, DISCONNECTED, CONNECTING, WAITING_FOR_HELLO, CONNECTED, ERROR
  }

  /**
   * Gets the [LoginResponse.LoginStatus] from when we connected. This will only be
   * interesting when [.getState] is [ConnectionState.ERROR].
   */

}