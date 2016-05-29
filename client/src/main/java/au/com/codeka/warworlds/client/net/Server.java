package au.com.codeka.warworlds.client.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.squareup.wire.WireField;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.BuildConfig;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Packet;

/** Represents our connection to the server. */
public class Server {
  private final static Log log = new Log("Server");

  private static final int DEFAULT_RECONNECT_TIME_MS = 1000;
  private static final int MAX_RECONNECT_TIME_MS = 30000;

  private final PacketDispatcher packetDispatcher = new PacketDispatcher();
  @NonNull private ServerStateEvent currState =
      new ServerStateEvent("", ServerStateEvent.ConnectionState.DISCONNECTED);

  /** The WebSocket we're connected to. Will be null if we're not connected. */
  @Nullable private WebSocket ws;

  /** A queue for storing packets while we attempt to reconnect. Will be null if we're connected. */
  @Nullable private Queue<Packet> queuedPackets = new ArrayDeque<>();

  /** A lock used to guard access to the web socket/queue. */
  private final Object lock = new Object();

  private int reconnectTimeMs = DEFAULT_RECONNECT_TIME_MS;

  /** Connect to the server. */
  public void connect() {
    String cookie = GameSettings.i.getString(GameSettings.Key.COOKIE);
    if (cookie.isEmpty()) {
      log.warning("No cookie yet, not connecting.");
      return;
    }
    GameSettings.i.addSettingChangedHandler(new GameSettings.SettingChangeHandler() {
      @Override
      public void onSettingChanged(GameSettings.Key key) {
        if (key == GameSettings.Key.SERVER) {
          // If you change SERVER, we'll want to clear the cookie.
          GameSettings.i.edit()
             .setString(GameSettings.Key.COOKIE, "")
             .commit();
        }
      }
    });

    log.info("Attempting to connect to: %s", ServerUrl.getWebSocketUrl());
    updateState(ServerStateEvent.ConnectionState.CONNECTING);

    WebSocketFactory factory = new WebSocketFactory();
    try {
      WebSocket newWebSocket = factory.createSocket(ServerUrl.getWebSocketUrl());
      newWebSocket.addHeader("X-Cookie", cookie);
      newWebSocket.addListener(webSocketListener);
      newWebSocket.setPingInterval(15000); // ping every 15 seconds.
      // TODO: re-enable this, but it seems broken when packets get fragmented.
      //newWebSocket.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
      newWebSocket.connectAsynchronously();
    } catch (IOException e) {
      log.error("Error connecting to server, will try again.", e);
      onDisconnect();
    }
  }

  @NonNull
  public ServerStateEvent getCurrState() {
    return currState;
  }

  public void send(Packet pkt) {
    synchronized (lock) {
      if (queuedPackets != null) {
        queuedPackets.add(pkt);
      } else if (ws != null) {
        byte[] bytes = pkt.encode();

        String packetName = getDebugPacketType(pkt);
        App.i.getEventBus().publish(new ServerPacketEvent(
            pkt, bytes, ServerPacketEvent.Direction.Sent, packetName));
        log.debug(">> %s (%d bytes)", packetName, bytes.length);

        ws.sendBinary(bytes);
      } else {
        throw new IllegalStateException("One of queuedPackets or ws should be non-null.");
      }
    }
  }

  private void onConnect(WebSocket ws) {
    synchronized (lock) {
      this.ws = ws;
      reconnectTimeMs = DEFAULT_RECONNECT_TIME_MS;
      // We are now in the "waiting for hello" state as we wait for the HelloResponse packet.
      updateState(ServerStateEvent.ConnectionState.WAITING_FOR_HELLO);

      Preconditions.checkNotNull(queuedPackets);
      while (!queuedPackets.isEmpty()) {
        send(queuedPackets.remove());
      }
      queuedPackets = null;
    }
  }

  private void onDisconnect() {
    synchronized (lock) {
      ws = null;
      queuedPackets = new ArrayDeque<>();
    }

    updateState(ServerStateEvent.ConnectionState.DISCONNECTED);
    App.i.getTaskRunner().runTask(new Runnable() {
      @Override
      public void run() {
        reconnectTimeMs *= 2;
        if (reconnectTimeMs > MAX_RECONNECT_TIME_MS) {
          reconnectTimeMs = MAX_RECONNECT_TIME_MS;
        }

        connect();
      }
    }, Threads.BACKGROUND, reconnectTimeMs);
  }

  private void onPacket(Packet pkt) {
    if (currState.getState().equals(ServerStateEvent.ConnectionState.WAITING_FOR_HELLO)) {
      if (pkt.hello != null) {
        // Now we're connected!
        EmpireManager.i.onHello(pkt.hello.empire);
        updateState(ServerStateEvent.ConnectionState.CONNECTED);
      } else {
        // Any other packet is unexpected in this state!
        log.warning("Unknown packet received while waiting for hello_response.");
      }
      return;
    }

    // Dispatch the packet to the event bus.
    packetDispatcher.dispatch(pkt);
  }

  private void updateState(ServerStateEvent.ConnectionState state) {
    currState = new ServerStateEvent(ServerUrl.getWebSocketUrl(), state);
    App.i.getEventBus().publish(currState);
  }

  /**
   * Useful for debugging, gets the "name" of the given packet, which is actually the name of the
   * type of the first non-null field in the packet.
   */
  private String getDebugPacketType(Packet pkt) {
    if (!BuildConfig.DEBUG) {
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

  private WebSocketListener webSocketListener = new WebSocketAdapter() {
    @Override
    public void onConnected(WebSocket ws, Map<String, List<String>> headers) throws Exception {
      log.debug("onConnected()");
      onConnect(ws);
    }

    @Override
    public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {
      log.debug("onConnectError(%s)", cause.getMessage());
      onDisconnect();
    }

    @Override
    public void onDisconnected(
        WebSocket ws, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame,
        boolean closedByServer) throws Exception {
      log.debug("onDisconnected(%s /* closedByServer */)", closedByServer ? "true" : "false");
      onDisconnect();
    }

    @Override
    public void onBinaryMessage(WebSocket ws, byte[] binary) throws Exception {
      log.debug("onBinaryMessage(%d bytes)", binary.length);
      Packet pkt = Packet.ADAPTER.decode(binary);

      String packetName = getDebugPacketType(pkt);
      App.i.getEventBus().publish(new ServerPacketEvent(pkt, binary,
          ServerPacketEvent.Direction.Received, packetName));
      log.debug("<< %s (%d bytes)", packetName, binary.length);

      onPacket(pkt);
    }


    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
      log.warning("onError()", cause);
    }


    @Override
    public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame)
        throws Exception {
      log.warning("onFrameError()", cause);
    }


    @Override
    public void onMessageError(WebSocket websocket, WebSocketException cause,
        List<WebSocketFrame> frames) throws Exception {
      log.warning("onMessageError()", cause);
    }

    @Override
    public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause,
        byte[] compressed) throws Exception {
      log.warning("onMessageDecompressionError(%d bytes)", compressed.length, cause);
    }

    @Override
    public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame)
        throws Exception {
      log.warning("onSendError()", cause);
    }

    @Override
    public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
      log.warning("onUnexpectedError()", cause);
    }

  };
}
