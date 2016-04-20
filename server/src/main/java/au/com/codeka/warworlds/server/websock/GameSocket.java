package au.com.codeka.warworlds.server.websock;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;

import au.com.codeka.warworlds.common.Log;

/**
 * Represents a socket connection to a single client, receives messages from them and sends messages
 * to them.
 */
public class GameSocket extends WebSocketAdapter {
  private static final Log log = new Log("GameSocket");

  @Override
  public void onWebSocketConnect(Session sess) {
    super.onWebSocketConnect(sess);
    log.info("Socket Connected: %s", sess);
  }

  @Override
  public void onWebSocketBinary(byte[] payload, int offset, int len) {
    super.onWebSocketBinary(payload, offset, len);
    log.info("Received BINARY message: %d bytes", len);
  }

  @Override
  public void onWebSocketText(String text) {
    super.onWebSocketText(text);
    log.info("Received TEXT message: %s", text);
    try {
      getRemote().sendString("Reply: " + text);
    } catch (IOException e) {
      log.error("Error", e);
    }
  }

  @Override
  public void onWebSocketClose(int statusCode, String reason) {
    super.onWebSocketClose(statusCode,reason);
    log.info("Socket Closed: [%d] %s", statusCode, reason);
  }

  @Override
  public void onWebSocketError(Throwable cause) {
    super.onWebSocketError(cause);
    cause.printStackTrace(System.err);
  }
}
