package au.com.codeka.warworlds.server.websock;

import com.squareup.wire.Message;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;
import java.nio.ByteBuffer;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Represents a socket connection to a single client, receives messages from them and sends messages
 * to them.
 */
public class GameSocket extends WebSocketAdapter {
  private static final Log log = new Log("GameSocket");

  private Session session;
  private final WatchableObject<Empire> empire;

  public GameSocket(String cookie, Account account) {
    empire = EmpireManager.i.getEmpire(account.empire_id);
    log.debug("Connection '%s' empire: %s", cookie, empire);
  }

  @Override
  public void onWebSocketConnect(Session sess) {
    super.onWebSocketConnect(sess);
    this.session = sess;
    log.info("Socket Connected: %s", sess);

    sendMessage(empire.get());
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

  private void sendMessage(Message<?, ?> msg) {
    try {
      getRemote().sendBytes(ByteBuffer.wrap(msg.encode()));
    } catch (IOException e) {
      log.error("Error sending message to client.", e);
    }
  }
}
