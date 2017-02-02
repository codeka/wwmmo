package au.com.codeka.warworlds.common.net;

import java.io.IOException;
import java.io.InputStream;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Packet;
import okio.BufferedSource;
import okio.Okio;

/**
 * Decodes a stream of {@link Packet}s, as encoded by {@PacketEncoder}.
 */
public class PacketDecoder {
  private static final Log log = new Log("PacketDecoder");

  public interface PacketHandler {
    void onPacket(PacketDecoder decoder, Packet pkt, int encodedSize);
    void onDisconnect();
  }

  private final BufferedSource source;
  private final Thread thread;

  private PacketHandler handler;

  public PacketDecoder(InputStream ins, PacketHandler handler) {
    this.source = Okio.buffer(Okio.source(ins));
    this.handler = handler;
    thread = new Thread(readRunnable);
    thread.start();
  }

  public void setPacketHandler(PacketHandler handler) {
    this.handler = handler;
  }

  private final Runnable readRunnable = new Runnable() {
    @Override
    public void run() {
      try {
        while (!source.exhausted()) {
          int size = source.readIntLe();
          int flags = source.readIntLe();
          byte[] bytes = source.readByteArray(size);
          if ((flags & PacketFlags.COMPRESSED) != 0) {
            bytes = GzipHelper.decompress(bytes);
          }
          Packet pkt = Packet.ADAPTER.decode(bytes);

          handler.onPacket(PacketDecoder.this, pkt, size);
        }
      } catch(IOException e) {
        log.warning("Error decoding packet.", e);
        handler.onDisconnect();
      }
    }
  };
}
