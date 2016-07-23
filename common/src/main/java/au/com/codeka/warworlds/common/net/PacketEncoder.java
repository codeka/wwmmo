package au.com.codeka.warworlds.common.net;

import com.google.common.base.Preconditions;
import com.squareup.wire.ProtoAdapter;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.proto.Packet;

/**
 * Encodes {@link Packet}s onto a stream so that {@link PacketDecoder} can decode them.
 */
public class PacketEncoder {
  public interface PacketHandler {
    void onPacket(Packet packet, int encodedSize);
  }

  private final OutputStream outs;
  private final Object lock = new Object();
  @Nullable private PacketHandler handler;

  public PacketEncoder(OutputStream outs) {
    this(outs, null);
  }

  public PacketEncoder(OutputStream outs, @Nullable PacketHandler handler) {
    this.outs = Preconditions.checkNotNull(outs);
    this.handler = handler;
  }


  public void setPacketHandler(@Nullable PacketHandler handler) {
    this.handler = handler;
  }

  public void send(Packet packet) throws IOException {
    int encodedSize = packet.adapter().encodedSize(packet);
    if (handler != null) {
      handler.onPacket(packet, encodedSize);
    }

    synchronized (lock) {
      ProtoAdapter.INT32.encode(outs, encodedSize);
      packet.encode(outs);
    }
  }
}
