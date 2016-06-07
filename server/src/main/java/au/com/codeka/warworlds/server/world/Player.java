package au.com.codeka.warworlds.server.world;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.ModifyStarPacket;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarUpdatedPacket;
import au.com.codeka.warworlds.common.proto.WatchSectorsPacket;
import au.com.codeka.warworlds.server.websock.GameSocket;

/**
 * Represents a currently-connected player.
 */
public class Player {
  private final GameSocket socket;
  /** The {@link Empire} this player belongs to. */
  private final WatchableObject<Empire> empire;

  /** The list of {@link Sector}s this player is currently watching. */
  private final ArrayList<WatchableObject<Sector>> sectors = new ArrayList<>();

  /** The {@link Star}s that we are currently watching. */
  private final Map<Long, WatchableObject<Star>> stars = new HashMap<>();

  /** The {@link WatchableObject.Watcher} which we'll be watching stars with. */
  private final WatchableObject.Watcher<Star> starWatcher;

  public Player(GameSocket socket, WatchableObject<Empire> empire) {
    this.socket = Preconditions.checkNotNull(socket);
    this.empire = Preconditions.checkNotNull(empire);

    starWatcher = star -> {
      socket.send(new Packet.Builder()
          .star_updated(new StarUpdatedPacket.Builder()
              .stars(Lists.newArrayList(star.get()))
              .build())
          .build());
    };
  }

  public void onPacket(Packet pkt) {
    if (pkt.watch_sectors != null) {
      onWatchSectorsPacket(pkt.watch_sectors);
    } else if (pkt.modify_star != null) {
      onModifyStar(pkt.modify_star);
    }
  }

  protected void onWatchSectorsPacket(WatchSectorsPacket pkt) {
    // TODO: if we're already watching some of these sectors, we can just keep watching those,

    // Remove all our current watched stars
    synchronized (stars) {
      for (WatchableObject<Star> star : stars.values()) {
        star.removeWatcher(starWatcher);
      }
    }

    sectors.clear();
    List<Star> stars = new ArrayList<>();
    for (long sectorY = pkt.top; sectorY <= pkt.bottom; sectorY ++) {
      for (long sectorX = pkt.left; sectorX <= pkt.right; sectorX ++) {
        WatchableObject<Sector> sector =
            SectorManager.i.getSector(new SectorCoord.Builder().x(sectorX).y(sectorY).build());
        sectors.add(sector);
        for (Star star : sector.get().stars) {
          stars.add(star);
        }
      }
    }

    socket.send(new Packet.Builder()
        .star_updated(new StarUpdatedPacket.Builder()
            .stars(stars)
            .build())
        .build());

    synchronized (this.stars) {
      for (Star star : stars) {
        WatchableObject<Star> watchableStar = StarManager.i.getStar(star.id);
        watchableStar.addWatcher(starWatcher);
        this.stars.put(star.id, watchableStar);
      }
    }
  }

  protected void onModifyStar(ModifyStarPacket pkt) {
    WatchableObject<Star> star = StarManager.i.getStar(pkt.star_id);
    StarManager.i.modifyStar(star, pkt.modification);
  }
}
