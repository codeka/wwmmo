package au.com.codeka.warworlds.server.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.debug.PacketDebug;
import au.com.codeka.warworlds.common.net.PacketDecoder;
import au.com.codeka.warworlds.common.net.PacketEncoder;
import au.com.codeka.warworlds.common.proto.Account;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.server.world.Player;
import au.com.codeka.warworlds.server.world.WatchableObject;

/**
 * Represents an established connection to a client.
 */
public class Connection implements PacketDecoder.PacketHandler {
  private final static Log log = new Log("Connection");

  private final Account account;
  private final WatchableObject<Empire> empire;
  private final byte[] encryptionKey;
  private final Socket socket;
  private final PacketEncoder encoder;
  private final PacketDecoder decoder;
  private final Player player;

  public Connection(
      Account account,
      WatchableObject<Empire> empire,
      byte[] encryptionKey,
      Socket socket,
      PacketDecoder decoder,
      OutputStream outs) {
    this.account = account;
    this.empire = empire;
    this.encryptionKey = encryptionKey;
    this.socket = socket;
    this.encoder = new PacketEncoder(outs);
    this.decoder = decoder;
    decoder.setPacketHandler(this);

    player = new Player(this, empire);
  }

  public void start() {
    encoder.setPacketHandler(packetEncodeHandler);
  }

  public void send(Packet pkt) {
    try {
      encoder.send(pkt);
    } catch (IOException e) {
      log.warning("Error", e);
    }
  }

  @Override
  public void onPacket(PacketDecoder decoder, Packet packet, int encodedSize) {
    if (log.isDebugEnabled()) {
      log.debug("<< [%d %s] %s", empire.get().id, empire.get().display_name,
          PacketDebug.getPacketDebug(packet, encodedSize));
    }
    player.onPacket(packet);
  }

  private PacketEncoder.PacketHandler packetEncodeHandler = new PacketEncoder.PacketHandler() {
    @Override
    public void onPacket(Packet packet, int encodedSize) {
      if (log.isDebugEnabled()) {
        log.debug(">> [%d %s] %s", empire.get().id, empire.get().display_name,
            PacketDebug.getPacketDebug(packet, encodedSize));
      }
    }
  };
}
