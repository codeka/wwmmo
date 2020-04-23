package au.com.codeka.warworlds.server.net

import au.com.codeka.warworlds.common.net.PacketDecoder
import au.com.codeka.warworlds.common.proto.Account
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.HelloPacket
import au.com.codeka.warworlds.server.world.WatchableObject
import java.io.OutputStream
import java.net.Socket

/**
 * Represents a pending connection, which is when you request /login but have not yet connected
 * to the game socket.
 */
class PendingConnection(
    private val manager: ServerSocketManager,
    private val account: Account,
    val empire: WatchableObject<Empire>,
    private val encryptionKey: ByteArray?) {

  /** Called when the user actually connects to the game socket, returns a [Connection].  */
  fun connect(
      helloPacket: HelloPacket,
      socket: Socket,
      decoder: PacketDecoder,
      outs: OutputStream?): Connection {
    val conn = Connection(
        manager,
        helloPacket,
        account,
        empire,
        encryptionKey,
        socket,
        decoder,
        outs)
    conn.start()
    return conn
  }

}