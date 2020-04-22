package au.com.codeka.warworlds.client.net

import au.com.codeka.warworlds.common.proto.Packet
import com.google.common.base.Preconditions

/**
 * An event that is fired every time a packet is sent or received.
 */
class ServerPacketEvent(packet: Packet?, encodedLength: Int, direction: Direction, packetDebug: String) {
  enum class Direction {
    Sent, Received
  }

  /** @return The actual [Packet] that was sent/received.
   */
  val packet: Packet?

  /** @return The length of the encoded packet.
   */
  val encodedLength: Int

  /** @return Whether the packet was sent or received.
   */
  val direction: Direction

  /** @return The a debug string for the packet, useful for debugging.
   */
  val packetDebug: String

  init {
    this.packet = Preconditions.checkNotNull(packet)
    this.encodedLength = encodedLength
    this.direction = Preconditions.checkNotNull(direction)
    this.packetDebug = Preconditions.checkNotNull(packetDebug)
  }
}