package au.com.codeka.warworlds.common.net

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.net.PacketDecoder
import au.com.codeka.warworlds.common.proto.Packet
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.IOException
import java.io.InputStream

/**
 * Decodes a stream of [Packet]s, as encoded by [PacketEncoder].
 */
class PacketDecoder(ins: InputStream, private var handler: PacketHandler) {
  interface PacketHandler {
    fun onPacket(decoder: PacketDecoder, pkt: Packet, encodedSize: Int)
    fun onDisconnect()
  }

  private val source: BufferedSource = ins.source().buffer()
  private val thread: Thread

  companion object {
    private val log = Log("PacketDecoder")
  }

  fun setPacketHandler(handler: PacketHandler) {
    this.handler = handler
  }

  private val readRunnable = Runnable {
    try {
      while (!source.exhausted()) {
        val size = source.readIntLe()
        val flags = source.readIntLe()
        var bytes: ByteArray? = source.readByteArray(size.toLong())
        if (flags and PacketFlags.COMPRESSED != 0) {
          bytes = GzipHelper.decompress(bytes)
        }
        val pkt = Packet.ADAPTER.decode(bytes!!)
        handler.onPacket(this@PacketDecoder, pkt, size)
      }
    } catch (e: IOException) {
      log.warning("Error decoding packet.", e)
      handler.onDisconnect()
    }
  }

  init {
    thread = Thread(readRunnable)
    thread.start()
  }
}