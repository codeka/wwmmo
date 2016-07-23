package au.com.codeka.warworlds.client.net;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import au.com.codeka.warworlds.client.App;
import au.com.codeka.warworlds.client.concurrency.Threads;
import au.com.codeka.warworlds.client.util.GameSettings;
import au.com.codeka.warworlds.client.world.EmpireManager;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.debug.PacketDebug;
import au.com.codeka.warworlds.common.net.PacketDecoder;
import au.com.codeka.warworlds.common.net.PacketEncoder;
import au.com.codeka.warworlds.common.proto.HelloPacket;
import au.com.codeka.warworlds.common.proto.LoginRequest;
import au.com.codeka.warworlds.common.proto.LoginResponse;
import au.com.codeka.warworlds.common.proto.Packet;

/** Represents our connection to the server. */
public class Server {
  private final static Log log = new Log("Server");

  private static final int DEFAULT_RECONNECT_TIME_MS = 1000;
  private static final int MAX_RECONNECT_TIME_MS = 30000;

  private final PacketDispatcher packetDispatcher = new PacketDispatcher();
  @Nonnull private ServerStateEvent currState =
      new ServerStateEvent("", ServerStateEvent.ConnectionState.DISCONNECTED);

  /** The socket that's connected to the server. Null if we're not connected. */
  @Nullable private Socket gameSocket;

  /** The PacketEncoder we'll use to send packets. Null if we're not connected. */
  @Nullable private PacketEncoder packetEncoder;

  /** The PacketDecoder we'll use to receive packets. Null if we're not connected. */
  @Nullable private PacketDecoder packetDecoder;

  /** A queue for storing packets while we attempt to reconnect. Will be null if we're connected. */
  @Nullable private Queue<Packet> queuedPackets = new ArrayDeque<>();

  /** A lock used to guard access to the web socket/queue. */
  private final Object lock = new Object();

  private int reconnectTimeMs = DEFAULT_RECONNECT_TIME_MS;

  /** Connect to the server. */
  public void connect() {
    final String cookie = GameSettings.i.getString(GameSettings.Key.COOKIE);
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

    updateState(ServerStateEvent.ConnectionState.CONNECTING);

    App.i.getTaskRunner().runTask(new Runnable() {
      @Override
      public void run() {
        log.info("Logging in: %s", ServerUrl.getLoginUrl());
        HttpRequest request = new HttpRequest.Builder()
            .url(ServerUrl.getLoginUrl())
            .method(HttpRequest.Method.POST)
            .body(new LoginRequest.Builder()
                .cookie(cookie)
                .build().encode())
            .build();
        LoginResponse loginResponse = request.getBody(LoginResponse.class);
        if (request.getResponseCode() != 200) {
          log.error("Error logging in, will try again.", request.getException());
          onDisconnect();
        } else {
          connectGameSocket(loginResponse);
        }
      }
    }, Threads.BACKGROUND);
  }

  private void connectGameSocket(LoginResponse loginResponse) {
    try {
      gameSocket = new Socket();
      gameSocket.connect(new InetSocketAddress(loginResponse.host, loginResponse.port));
      packetEncoder = new PacketEncoder(gameSocket.getOutputStream(), packetEncodeHandler);
      packetDecoder = new PacketDecoder(gameSocket.getInputStream(), packetDecodeHandler);

      Queue<Packet> oldQueuedPackets = Preconditions.checkNotNull(queuedPackets);
      queuedPackets = null;

      send(new Packet.Builder()
          .hello(new HelloPacket.Builder()
              .empire_id(loginResponse.empire.id)
              .build())
          .build());

      EmpireManager.i.onHello(loginResponse.empire);

      reconnectTimeMs = DEFAULT_RECONNECT_TIME_MS;
      updateState(ServerStateEvent.ConnectionState.CONNECTED);

      while (!oldQueuedPackets.isEmpty()) {
        send(oldQueuedPackets.remove());
      }
    } catch (IOException e) {
      gameSocket = null;
      log.error("Error connecting game socket, will try again.", e);
      onDisconnect();
    }
  }

  @Nonnull
  public ServerStateEvent getCurrState() {
    return currState;
  }

  public void send(Packet pkt) {
    synchronized (lock) {
      if (queuedPackets != null) {
        queuedPackets.add(pkt);
      } else {
        Preconditions.checkNotNull(packetEncoder);
        try {
          packetEncoder.send(pkt);
        } catch (IOException e) {
          // TODO: handle error
          onDisconnect();
        }
      }
    }
  }

  private void onDisconnect() {

    synchronized (lock) {
      Preconditions.checkNotNull(gameSocket);
      try {
        gameSocket.close();
      } catch (IOException e) {
        // ignore.
      }
      gameSocket = null;
      packetEncoder = null;
      packetDecoder = null;
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

  private final PacketEncoder.PacketHandler packetEncodeHandler =
      new PacketEncoder.PacketHandler() {
    @Override
    public void onPacket(Packet packet, int encodedSize) {
      String packetDebug = PacketDebug.getPacketDebug(packet, encodedSize);
      App.i.getEventBus().publish(new ServerPacketEvent(
          packet, encodedSize, ServerPacketEvent.Direction.Sent, packetDebug));
      log.debug(">> %s", packetDebug);
    }
  };

  private final PacketDecoder.PacketHandler packetDecodeHandler =
      new PacketDecoder.PacketHandler() {
    @Override
    public void onPacket(PacketDecoder decoder, Packet packet, int encodedSize) {
      String packetDebug = PacketDebug.getPacketDebug(packet, encodedSize);
      App.i.getEventBus().publish(new ServerPacketEvent(
          packet, encodedSize, ServerPacketEvent.Direction.Received, packetDebug));
      log.debug("<< %s", packetDebug);

      packetDispatcher.dispatch(packet);
    }
  };

  private void updateState(ServerStateEvent.ConnectionState state) {
    currState = new ServerStateEvent(ServerUrl.getUrl(), state);
    App.i.getEventBus().publish(currState);
  }
}
