package au.com.codeka.warworlds.client.net;

import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.BuildConfig;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.util.eventbus.EventBus;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Packet;

/** Represents our connection to the server. */
public class Server {
  private final static Log log = new Log("Server");

  private static final int DEFAULT_RECONNECT_TIME_MS = 1000;
  private static final int MAX_RECONNECT_TIME_MS = 30000;

  private final String url = BuildConfig.WEBSOCKET_URL;
  private ServerStateEvent currState;

  /** The WebSocket we're connected to. Will be null if we're not connected. */
  @Nullable private WebSocket ws;

  /** A queue for storing packets while we attempt to reconnect. Will be null if we're connected. */
  @Nullable private Queue<Packet> queuedPackets = new ArrayDeque<>();

  /** A lock used to guard access to the web socket/queue. */
  private final Object lock = new Object();

  /** The cookie we use to authenticate. Null if we don't have a cookie yet. */
  @Nullable private String cookie;

  private int reconnectTimeMs = DEFAULT_RECONNECT_TIME_MS;

  /** Connect to the server. */
  public void connect() {
    cookie = GameSettings.i.getString(GameSettings.Key.COOKIE);
    if (cookie.isEmpty()) {
      log.warning("No cookie yet, not connecting.");
      return;
    }

    log.info("Attempting to connect to: %s", url);
    updateState(ServerStateEvent.ConnectionState.CONNECTING);

    WebSocketFactory factory = new WebSocketFactory();
    try {
      WebSocket newWebSocket = factory.createSocket(url);
      newWebSocket.addHeader("X-Cookie", cookie);
      newWebSocket.addListener(webSocketListener);
      //ws.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
      newWebSocket.connectAsynchronously();
    } catch (IOException e) {
      log.error("Error connecting to server, will try again.", e);
      onDisconnect();
    }
  }

  public ServerStateEvent getCurrState() {
    return currState;
  }

  private void send(Packet pkt) {
    synchronized (lock) {
      if (queuedPackets != null) {
        queuedPackets.add(pkt);
      } else if (ws != null) {
        ws.sendBinary(pkt.encode());
      } else {
        throw new IllegalStateException("One of queuedPackets or ws should be non-null.");
      }
    }
  }

  private void onConnect(WebSocket ws) {
    synchronized (lock) {
      this.ws = ws;
      reconnectTimeMs = DEFAULT_RECONNECT_TIME_MS;
      updateState(ServerStateEvent.ConnectionState.CONNECTED);

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
    log.debug("Packet: %s", pkt);
  }

  private void updateState(ServerStateEvent.ConnectionState state) {
    currState = new ServerStateEvent(url, state);
    App.i.getEventBus().publish(currState);
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
      onPacket(Packet.ADAPTER.decode(binary));
    }
  };
}
