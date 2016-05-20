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
  private final byte[] bytes;
  private final Direction direction;
  private final String packetName;

  public ServerPacketEvent(Packet packet, byte[] bytes, Direction direction, String packetName) {
    this.packet = Preconditions.checkNotNull(packet);
    this.bytes = Preconditions.checkNotNull(bytes);
    this.direction = Preconditions.checkNotNull(direction);
    this.packetName = Preconditions.checkNotNull(packetName);
  }

  /** @return The actual {@link Packet} that was sent/received. */
  public Packet getPacket() {
    return packet;
  }

  /** @return The serialized packet. */
  public byte[] getBytes() {
    return bytes;
  }

  /** @return Whether the packet was sent or received. */
  public Direction getDirection() {
    return direction;
  }

  /** @return The "name" of the packet, useful for debugging. */
  public String getPacketName() {
    return packetName;
  }
}
