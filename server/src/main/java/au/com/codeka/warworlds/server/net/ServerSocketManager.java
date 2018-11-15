package au.com.codeka.warworlds.server.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.TreeMap;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.net.PacketDecoder;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Manages the {@link ServerSocket} which is listening for clients to connect.
 */
public class ServerSocketManager {
  private static final Log log = new Log("ServerSocketManager");
  public static final ServerSocketManager i = new ServerSocketManager();

  private ServerSocket serverSocket;
  private Thread acceptThread;

  private final Map<Long, PendingConnection> pendingConnections = new TreeMap<>();
  private final Map<Long, Connection> connections = new TreeMap<>();

  public boolean start() {
    try {
      serverSocket = new ServerSocket(8081); // TODO: configurable
    } catch (IOException e) {
      log.error("Error starting socket server.", e);
      return false;
    }

    acceptThread = new Thread(this::acceptThreadProc);
    acceptThread.start();
    return true;
  }

  /**
   * Adds a pending connection from the given {@link Account}, with the given expected encryption
   * key. If nothing connects within CONNECTION_TIMEOUT_MS, we'll drop this pending connection.
   *
   * @param account The {@link Account} that's connecting.
   * @param empire The {@link Empire} that's connecting.
   * @param encryptionKey The player's encryption key.
   */
  public void addPendingConnection(
      Account account, WatchableObject<Empire> empire, byte[] encryptionKey) {
    pendingConnections.put(
        empire.get().id, new PendingConnection(this, account, empire, encryptionKey));
  }

  public void stop() {
    log.info("Server socket stopping.");

    try {
      serverSocket.close();
    } catch (IOException e) {
      log.error("Error stopping socket server.", e);
    }
    serverSocket = null;

    try {
      acceptThread.join();
    } catch (InterruptedException e) {
      // ignore
    }
    acceptThread = null;
  }

  /** Called by the {@link Connection} when it disconnects. */
  void onDisconnect(Long empireId, Connection conn) {
    connections.remove(empireId);
  }

  /** Called when we get a new connection from a client. */
  private void handleConnection(Socket socket) {
    InputStream ins;
    OutputStream outs;
    try {
      ins = socket.getInputStream();
      outs = socket.getOutputStream();
    } catch (IOException e) {
      log.error("Error waiting for 'hello'.", e);
      return;
    }

    new PacketDecoder(ins, new PendingConnectionPacketHandler(socket, outs));
  }

  /**
   * This class receives the first packet from a pending connection and then converts it to a
   * normal connection.
   */
  private class PendingConnectionPacketHandler implements PacketDecoder.PacketHandler {
    private final Socket socket;
    private final OutputStream outs;

    public PendingConnectionPacketHandler(Socket socket, OutputStream outs) {
      this.socket = socket;
      this.outs = outs;
    }

    @Override
    public void onPacket(PacketDecoder decoder, Packet pkt, int encodedSize) {
      if (pkt.hello == null) {
        log.error("Expected 'hello' packet, but didn't get it.");
        return;
      }

      PendingConnection pendingConnection = pendingConnections.remove(pkt.hello.empire_id);
      if (pendingConnection == null) {
        log.error("Got 'hello' packet, but no pending connection for empire #%d",
            pkt.hello.empire_id);
        return;
      }

      log.info("GameSocket connection received for empire #%d %s",
          pkt.hello.empire_id, pendingConnection.getEmpire().get().display_name);
      connections.put(
          pkt.hello.empire_id,
          pendingConnection.connect(pkt.hello, socket, decoder, outs));
    }

    @Override
    public void onDisconnect() {
      log.warning("Client disconnected while waiting for 'hello' packet.");
    }
  }

  private void acceptThreadProc() {
    while (true) {
      try {
        Socket socket = serverSocket.accept();
        log.debug("Socket accepted from %s", socket.getRemoteSocketAddress());
        handleConnection(socket);
      } catch (IOException e) {
        log.error("Error accepting connection.", e);
        return;
      }
    }
  }
}
