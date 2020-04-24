package au.com.codeka.warworlds.server.net

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.net.PacketDecoder
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.Packet
import au.com.codeka.warworlds.server.world.WatchableObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*

/**
 * Manages the [ServerSocket] which is listening for clients to connect.
 */
class ServerSocketManager {
  private lateinit var serverSocket: ServerSocket
  private lateinit var acceptThread: Thread
  private var closing = false
  private val pendingConnections: MutableMap<Long, PendingConnection> = TreeMap()
  private val connections: MutableMap<Long, Connection?> = TreeMap()

  fun start(): Boolean {
    serverSocket = try {
      ServerSocket(8081) // TODO: configurable
    } catch (e: IOException) {
      log.error("Error starting socket server.", e)
      return false
    }
    acceptThread = Thread(Runnable { acceptThreadProc() })
    acceptThread.start()
    closing = false
    return true
  }

  /**
   * Adds a pending connection from the given [Account], with the given expected encryption
   * key. If nothing connects within CONNECTION_TIMEOUT_MS, we'll drop this pending connection.
   *
   * @param account The [Account] that's connecting.
   * @param empire The [Empire] that's connecting.
   * @param encryptionKey The player's encryption key.
   */
  fun addPendingConnection(
      account: Account, empire: WatchableObject<Empire>, encryptionKey: ByteArray?) {
    pendingConnections[empire.get().id] = PendingConnection(this, account, empire, encryptionKey)
  }

  fun stop() {
    log.info("Server socket stopping.")
    closing = true
    try {
      serverSocket.close()
    } catch (e: IOException) {
      log.error("Error stopping socket server.", e)
    }
    try {
      acceptThread.join()
    } catch (e: InterruptedException) {
      // ignore
    }
  }

  /** Called by the [Connection] when it disconnects.  */
  fun onDisconnect(empireId: Long) {
    connections.remove(empireId)
  }

  /** Called when we get a new connection from a client.  */
  private fun handleConnection(socket: Socket) {
    val ins: InputStream
    val outs: OutputStream
    try {
      ins = socket.getInputStream()
      outs = socket.getOutputStream()
    } catch (e: IOException) {
      log.error("Error waiting for 'hello'.", e)
      return
    }
    PacketDecoder(ins, PendingConnectionPacketHandler(socket, outs))
  }

  /**
   * This class receives the first packet from a pending connection and then converts it to a
   * normal connection.
   */
  private inner class PendingConnectionPacketHandler
      internal constructor(private val socket: Socket,
                           private val outs: OutputStream) : PacketDecoder.PacketHandler {

    override fun onPacket(decoder: PacketDecoder, pkt: Packet, encodedSize: Int) {
      if (pkt.hello == null) {
        log.error("Expected 'hello' packet, but didn't get it.")
        return
      }
      val pendingConnection = pendingConnections.remove(pkt.hello.empire_id)
      if (pendingConnection == null) {
        log.error("Got 'hello' packet, but no pending connection for empire #%d",
            pkt.hello.empire_id)
        return
      }
      log.info("GameSocket connection received for empire #%d %s",
          pkt.hello.empire_id, pendingConnection.empire.get().display_name)
      connections[pkt.hello.empire_id] = pendingConnection.connect(pkt.hello, socket, decoder, outs)
    }

    override fun onDisconnect() {
      log.warning("Client disconnected while waiting for 'hello' packet.")
    }
  }

  private fun acceptThreadProc() {
    while (true) {
      try {
        val socket = serverSocket.accept()
        log.debug("Socket accepted from %s", socket.remoteSocketAddress)
        handleConnection(socket)
      } catch (e: IOException) {
        if (!closing) {
          log.error("Error accepting connection.", e)
        }
        return
      }
    }
  }

  companion object {
    private val log = Log("ServerSocketManager")
    val i = ServerSocketManager()
  }
}