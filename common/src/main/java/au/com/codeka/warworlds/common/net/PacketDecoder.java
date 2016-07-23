package au.com.codeka.warworlds.common.net;

import com.google.common.base.Preconditions;
import com.squareup.wire.ProtoAdapter;

import java.io.IOException;
import java.io.InputStream;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Packet;
import okio.Buffer;
import okio.Okio;
import okio.Source;
import okio.Timeout;

/**
 * Decodes a stream of {@link Packet}s, as encoded by {@PacketEncoder}.
 */
public class PacketDecoder {
  private static final Log log = new Log("PacketDecoder");

  public interface PacketHandler {
    void onPacket(PacketDecoder decoder, Packet pkt);
  }

  private final InputStream ins;
  private final Thread thread;

  private PacketHandler handler;

  /** A buffer that {@link FixedSizeSource} can use. */
  private final byte[] buffer = new byte[512];

  public PacketDecoder(InputStream ins, PacketHandler handler) {
    this.ins = Preconditions.checkNotNull(ins);
    this.handler = handler;
    thread = new Thread(readRunnable);
  }

  public void setPacketHandler(PacketHandler handler) {
    this.handler = handler;
  }

  private final Runnable readRunnable = new Runnable() {
    @Override
    public void run() {
      while (true) {
        try {
          handler.onPacket(PacketDecoder.this, decode());
        } catch(IOException e) {
          log.warning("Error decoding packet.");
          return;
        }
      }
    }
  };

  private Packet decode() throws IOException {
    int size = ProtoAdapter.INT32.decode(ins);
    return Packet.ADAPTER.decode(Okio.buffer(new FixedSizeSource(ins, size)));
  }

  /** A {@link Source} which can be used to read an exact number of bytes from a stream. */
  private class FixedSizeSource implements Source {
    private final InputStream ins;
    private final int size;
    private int bytesLeft;

    public FixedSizeSource(InputStream ins, int size) {
      this.ins = ins;
      this.size = size;
      this.bytesLeft = size;
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
      if (bytesLeft == 0) {
        return -1;
      }

      int toread = (int) Math.max(byteCount, bytesLeft);
      int read = 0;
      while (toread > 0) {
        int num = ins.read(buffer, 0, Math.max(buffer.length, toread));
        if (num > 0) {
          sink.write(buffer, 0, num);
          toread -= num;
          read += num;
        } else {
          break;
        }
      }

      return read;
    }

    @Override
    public Timeout timeout() {
      return Timeout.NONE;
    }

    @Override
    public void close() throws IOException {
    }
  }
}
