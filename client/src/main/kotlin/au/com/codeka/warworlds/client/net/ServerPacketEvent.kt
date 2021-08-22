package au.com.codeka.warworlds.client.net

import au.com.codeka.warworlds.common.proto.Packet
import com.google.common.base.Preconditions

/**
 * An event that is fired every time a packet is sent or received.
 */
class ServerPacketEvent(
    val packet: Packet?, val encodedLength: Int, val direction: Direction, val packetDebug: String
) {
  enum class Direction {
    Sent, Received
  }
}