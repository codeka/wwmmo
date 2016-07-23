package au.com.codeka.warworlds.common.net;

import com.google.common.base.Preconditions;
import com.squareup.wire.ProtoAdapter;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Packet;
import okio.BufferedSink;
import okio.Okio;

/**
 * Encodes {@link Packet}s onto a stream so that {@link PacketDecoder} can decode them.
 */
public class PacketEncoder {
  private final static Log log = new Log("PacketEncoder");

  public interface PacketHandler {
    void onPacket(Packet packet, int encodedSize);
  }

  private final BufferedSink sink;
  private final Object lock = new Object();
  @Nullable private PacketHandler handler;

  public PacketEncoder(OutputStream outs) {
    this(outs, null);
  }

  public PacketEncoder(OutputStream outs, @Nullable PacketHandler handler) {
    this.sink = Okio.buffer(Okio.sink(outs));
    this.handler = handler;
  }

  public void setPacketHandler(@Nullable PacketHandler handler) {
    this.handler = handler;
  }

  public void send(Packet packet) throws IOException {
    int size = packet.adapter().encodedSize(packet);
    synchronized (lock) {
      sink.writeIntLe(size);
      packet.encode(sink);
      sink.emit();
    }

    if (handler != null) {
      handler.onPacket(packet, size);
    }
  }
}
