package au.com.codeka.warworlds.server.world;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireDetailsPacket;
import au.com.codeka.warworlds.common.proto.ModifyStarPacket;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.common.proto.RequestEmpirePacket;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarUpdatedPacket;
import au.com.codeka.warworlds.common.proto.WatchSectorsPacket;
import au.com.codeka.warworlds.server.concurrency.TaskRunner;
import au.com.codeka.warworlds.server.concurrency.Threads;
import au.com.codeka.warworlds.server.net.Connection;

/** Represents a currently-connected player. */
public class Player {
  private static final Log log = new Log("Player");

  private final Connection connection;

  /** The {@link Empire} this player belongs to. */
  private final WatchableObject<Empire> empire;

  /** The list of {@link Sector}s this player is currently watching. */
  private final ArrayList<WatchableObject<Sector>> sectors = new ArrayList<>();

  /** The {@link Star}s that we are currently watching. */
  private final Map<Long, WatchableObject<Star>> stars = new HashMap<>();

  /** The {@link WatchableObject.Watcher} which we'll be watching stars with. */
  private final WatchableObject.Watcher<Star> starWatcher;

  public Player(Connection connection, WatchableObject<Empire> empire) {
    log.setPrefix(String.format(Locale.US, "[%d %s]", empire.get().id, empire.get().display_name));

    this.connection = Preconditions.checkNotNull(connection);
    this.empire = Preconditions.checkNotNull(empire);

    starWatcher = star -> {
      connection.send(new Packet.Builder()
          .star_updated(new StarUpdatedPacket.Builder()
              .stars(Lists.newArrayList(star.get()))
              .build())
          .build());
    };

    TaskRunner.i.runTask(this::onPostConnect, Threads.BACKGROUND);
  }

  public void onPacket(Packet pkt) {
    if (pkt.watch_sectors != null) {
      onWatchSectorsPacket(pkt.watch_sectors);
    } else if (pkt.modify_star != null) {
      onModifyStar(pkt.modify_star);
    } else if (pkt.request_empire != null) {
      onRequestEmpire(pkt.request_empire);
    }
  }

  /**
   * This is called on a background thread when this {@link Player} is created. We'll send the
   * client some updates they might be interested in.
   */
  private void onPostConnect() {
    long startTime = System.nanoTime();
    ArrayList<WatchableObject<Star>> stars = StarManager.i.getStarsForEmpire(empire.get().id);
    log.debug("Fetched %d stars for empire %d in %dms", stars.size(), empire.get().id,
        (System.nanoTime() - startTime) / 1000000L);

    // TODO: send to player
  }

  private void onWatchSectorsPacket(WatchSectorsPacket pkt) {
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
        SectorManager.i.verifyNativeColonies(sector);
        sectors.add(sector);
        stars.addAll(sector.get().stars);
      }
    }

    connection.send(new Packet.Builder()
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

  private void onModifyStar(ModifyStarPacket pkt) {
    WatchableObject<Star> star = StarManager.i.getStar(pkt.star_id);
    StarManager.i.modifyStar(star, pkt.modification, null /* logHandler */);
  }

  private void onRequestEmpire(RequestEmpirePacket pkt) {
    List<Empire> empires = new ArrayList<>();
    for (long id : pkt.empire_id) {
      WatchableObject<Empire> empire = EmpireManager.i.getEmpire(id);
      empires.add(empire.get());
    }
    connection.send(new Packet.Builder()
        .empire_details(new EmpireDetailsPacket.Builder()
            .empires(empires)
            .build())
        .build());
  }
}
