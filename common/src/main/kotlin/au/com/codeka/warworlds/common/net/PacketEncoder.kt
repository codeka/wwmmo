package au.com.codeka.warworlds.common.net

import au.com.codeka.warworlds.common.proto.Packet
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.OutputStream

/** Encodes [Packet]s onto a stream so that [PacketDecoder] can decode them. */
class PacketEncoder constructor(outs: OutputStream, private var handler: PacketHandler) {
  interface PacketHandler {
    fun onPacket(packet: Packet, encodedSize: Int)
  }

  private val sink: BufferedSink = outs.sink().buffer()
  private val lock = Any()

  fun setPacketHandler(handler: PacketHandler) {
    this.handler = handler
  }

  fun send(packet: Packet) {
    var bytes: ByteArray
    synchronized(lock) {
      var flags = PacketFlags.NONE
      bytes = packet.encode()
      val compressed = GzipHelper.compress(bytes)
      if (compressed != null && compressed.size < bytes.size) {
        flags = flags or PacketFlags.COMPRESSED
        bytes = compressed
      }
      sink.writeIntLe(bytes.size)
      sink.writeIntLe(flags)
      sink.write(bytes)
      sink.emit()
    }
    handler.onPacket(packet, bytes.size)
  }
}
