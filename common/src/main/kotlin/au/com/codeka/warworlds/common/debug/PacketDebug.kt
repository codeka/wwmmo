package au.com.codeka.warworlds.common.debug

import au.com.codeka.warworlds.common.proto.Packet
import com.squareup.wire.WireField

/**
 * Helper class that contains some nice debugging details about our packets.
 */
object PacketDebug {
  fun getPacketDebug(pkt: Packet): String {
    return getPacketDebug(pkt, -1)
  }

  fun getPacketDebug(pkt: Packet, serializedLength: Int): String {
    val sb = StringBuilder()
    for (field in pkt.javaClass.fields) {
      if (field.isAnnotationPresent(WireField::class.java)) {
        try {
          if (field[pkt] != null) {
            sb.append(field.type.simpleName)
            sb.append(" ")
          }
        } catch (e: IllegalAccessException) {
          // Ignore. (though should never happen)
        }
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