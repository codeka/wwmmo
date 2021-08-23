package au.com.codeka.warworlds.common.debug

import au.com.codeka.warworlds.common.net.PacketHelper
import au.com.codeka.warworlds.common.proto.Packet

/**
 * Helper class that contains some nice debugging details about our packets.
 */
object PacketDebug {
  fun getPacketDebug(pkt: Packet): String {
    return getPacketDebug(pkt, -1)
  }

  // Wire's generator doesn't do anything special for oneof fields (we don't get an enum like in
  // Google's C++ or Java generator) so we have to manually inspect the packet.
  fun getPacketDebug(pkt: Packet, serializedLength: Int): String {
    val sb = StringBuilder()

    for (prop in PacketHelper.properties.values) {
      if (prop.getter.call(pkt) != null) {
        sb.append(prop.name)
        sb.append(" ")
      }
    }
    if (serializedLength > 0) {
      sb.append("(")
      sb.append(serializedLength)
      sb.append(" bytes)")
    }
    if (pkt.watch_sectors != null) {
      sb.append(" : [")
      sb.append(pkt.watch_sectors.left)
      sb.append(",")
      sb.append(pkt.watch_sectors.top)
      sb.append("] [")
      sb.append(pkt.watch_sectors.right)
      sb.append(",")
      sb.append(pkt.watch_sectors.bottom)
      sb.append("]")
    }
    if (pkt.star_updated != null) {
      sb.append(" : ")
      sb.append(pkt.star_updated.stars.size)
      sb.append(" stars")
    }
    return sb.toString()
  }
}