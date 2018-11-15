package au.com.codeka.warworlds.client.net;

import com.google.common.base.Preconditions;

import au.com.codeka.warworlds.common.proto.Packet;

/**
 * An event that is fired every time a packet is sent or received.
 */
public class ServerPacketEvent {
  public enum Direction {
    Sent,
    Received
  }

  private final Packet packet;
  private final int encodedLength;
  private final Direction direction;
  private final String packetDebug;

  public ServerPacketEvent(Packet packet, int encodedLength, Direction direction, String packetDebug) {
    this.packet = Preconditions.checkNotNull(packet);
    this.encodedLength = encodedLength;
    this.direction = Preconditions.checkNotNull(direction);
    this.packetDebug = Preconditions.checkNotNull(packetDebug);
  }

  /** @return The actual {@link Packet} that was sent/received. */
  public Packet getPacket() {
    return packet;
  }

  /** @return The length of the encoded packet. */
  public int getEncodedLength() {
    return encodedLength;
  }

  /** @return Whether the packet was sent or received. */
  public Direction getDirection() {
    return direction;
  }

  /** @return The a debug string for the packet, useful for debugging. */
  public String getPacketDebug() {
    return packetDebug;
  }
}
