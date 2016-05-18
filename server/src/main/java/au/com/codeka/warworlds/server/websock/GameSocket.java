package au.com.codeka.warworlds.server.websock;

import com.squareup.wire.WireField;

import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.HelloPacket;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.Player;
import au.com.codeka.warworlds.server.world.WatchableObject;
import okio.ByteString;

/**
 * Represents a socket connection to a single client, receives messages from them and sends messages
 * to them.
 */
public class GameSocket extends WebSocketAdapter {
  private static final Log log = new Log("GameSocket");

  private Session session;
  private final WatchableObject<Empire> empire;
  private final Player player;

  public GameSocket(String cookie, Account account) {
    empire = EmpireManager.i.getEmpire(account.empire_id);
    player = new Player(this, empire);
    log.debug("Connection '%s' empire: %s", cookie, empire);
  }

  @Override
  public void onWebSocketConnect(Session sess) {
    super.onWebSocketConnect(sess);
    this.session = sess;
    log.info("Socket Connected: %s", sess);

    send(new Packet.Builder().hello(
        new HelloPacket.Builder()
            .empire(empire.get())
            .build()));
  }

  @Override
  public void onWebSocketBinary(byte[] payload, int offset, int len) {
    super.onWebSocketBinary(payload, offset, len);
    Packet pkt;
    try {
      pkt = Packet.ADAPTER.decode(ByteString.of(payload, offset, len));
    } catch (IOException e) {
      log.error("Error decoding %d bytes into Packet.", len, e);
      return;
    }
    log.debug("<< %s (%d bytes)", getPacketType(pkt), len);
    player.onPacket(pkt);
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

  public void send(Packet.Builder pktBuilder) {
    send(pktBuilder.build());
  }

  public void send(Packet pkt) {
    byte[] bytes = pkt.encode();
    log.debug(">> %s (%d bytes)", getPacketType(pkt), bytes.length);

    try {
      getRemote().sendBytes(ByteBuffer.wrap(bytes));
    } catch (IOException e) {
      log.error("Error sending message to client.", e);
    }
  }

  private String getPacketType(Packet pkt) {
    if (!log.isDebugEnabled()) {
      return "Packet";
    }

    for (Field field : pkt.getClass().getFields()) {
      if (field.isAnnotationPresent(WireField.class)) {
        try {
          if (field.get(pkt) != null) {
            return field.getType().getSimpleName();
          }
        } catch (IllegalAccessException e) {
          // Ignore. (though should never happen)
        }
      }
    }

    return "UKNOWN_PACKET";
  }
}
