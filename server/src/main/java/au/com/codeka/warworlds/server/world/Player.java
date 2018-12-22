package au.com.codeka.warworlds.server.world;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.debug.PacketDebug;
import au.com.codeka.warworlds.common.proto.ChatMessage;
import au.com.codeka.warworlds.common.proto.ChatMessagesPacket;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.EmpireDetailsPacket;
import au.com.codeka.warworlds.common.proto.HelloPacket;
import au.com.codeka.warworlds.common.proto.ModifyStarPacket;
import au.com.codeka.warworlds.common.proto.Packet;
import au.com.codeka.warworlds.common.proto.RequestEmpirePacket;
import au.com.codeka.warworlds.common.proto.Sector;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.common.proto.Star;
import au.com.codeka.warworlds.common.proto.StarModification;
import au.com.codeka.warworlds.common.proto.StarUpdatedPacket;
import au.com.codeka.warworlds.common.proto.WatchSectorsPacket;
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException;
import au.com.codeka.warworlds.server.concurrency.TaskRunner;
import au.com.codeka.warworlds.server.concurrency.Threads;
import au.com.codeka.warworlds.server.net.Connection;
import au.com.codeka.warworlds.server.world.chat.ChatManager;
import au.com.codeka.warworlds.server.world.chat.Participant;

import static com.google.common.base.Preconditions.checkNotNull;

/** Represents a currently-connected player. */
public class Player {
  private static final Log log = new Log("Player");

  private final Connection connection;
  private final HelloPacket helloPacket;

  /** The {@link Empire} this player belongs to. */
  private final WatchableObject<Empire> empire;

  /** The list of {@link Sector}s this player is currently watching. */
  private final ArrayList<WatchableObject<Sector>> watchedSectors = new ArrayList<>();

  /** The {@link Star}s that we are currently watching. */
  private final Map<Long, WatchableObject<Star>> watchedStars = new HashMap<>();

  /** The {@link WatchableObject.Watcher} which we'll be watching stars with. */
  private final WatchableObject.Watcher<Star> starWatcher;

  public Player(Connection connection, HelloPacket helloPacket, WatchableObject<Empire> empire) {
    log.setPrefix(String.format(Locale.US, "[%d %s]", empire.get().id, empire.get().display_name));

    this.helloPacket = checkNotNull(helloPacket);
    this.connection = checkNotNull(connection);
    this.empire = checkNotNull(empire);

    starWatcher = star -> connection.send(new Packet.Builder()
        .star_updated(new StarUpdatedPacket.Builder()
            .stars(Lists.newArrayList(star.get()))
            .build())
        .build());

    TaskRunner.i.runTask(this::onPostConnect, Threads.BACKGROUND);
  }

  public void onPacket(Packet pkt) {
    if (pkt.watch_sectors != null) {
      onWatchSectorsPacket(pkt.watch_sectors);
    } else if (pkt.modify_star != null) {
      onModifyStar(pkt.modify_star);
    } else if (pkt.request_empire != null) {
      onRequestEmpire(pkt.request_empire);
    } else if (pkt.chat_msgs != null) {
      onChatMessages(pkt.chat_msgs);
    } else {
      log.error("Unknown/unexpected packet. %s", PacketDebug.getPacketDebug(pkt));
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

    // Of the player's stars, send them all the ones that have been updated since their
    // last_simulation.
    ArrayList<Star> updatedStars = new ArrayList<>();
    for (WatchableObject<Star> star : stars) {
      if (helloPacket.our_star_last_simulation == null
          || (star.get().last_simulation != null
              && star.get().last_simulation > helloPacket.our_star_last_simulation)) {
        updatedStars.add(star.get());
      }
    }
    if (!updatedStars.isEmpty()) {
      log.debug("%d updated stars, sending update packet.", updatedStars.size());
      connection.send(new Packet.Builder()
          .star_updated(new StarUpdatedPacket.Builder()
              .stars(updatedStars)
              .build())
          .build());
    } else {
      log.debug("No updated stars, not sending update packet.");
    }

    // Register this player with the chat system so that we get notified of messages.
    ChatManager.i.connectPlayer(empire.get().id, helloPacket.last_chat_time, chatCallback);
  }

  /**
   * Called when the client disconnects from us.
   */
  public void onDisconnect() {
    ChatManager.i.disconnectPlayer(empire.get().id);

    clearWatchedStars();
    synchronized (watchedSectors) {
      watchedSectors.clear();
    }
  }

  private void onWatchSectorsPacket(WatchSectorsPacket pkt) {
    // TODO: if we're already watching some of these sectors, we can just keep watching those,

    // Remove all our current watched stars
    clearWatchedStars();

    List<Star> stars = new ArrayList<>();
    synchronized (watchedSectors) {
      watchedSectors.clear();
      for (long sectorY = pkt.top; sectorY <= pkt.bottom; sectorY++) {
        for (long sectorX = pkt.left; sectorX <= pkt.right; sectorX++) {
          WatchableObject<Sector> sector =
              SectorManager.i.getSector(new SectorCoord.Builder().x(sectorX).y(sectorY).build());
          SectorManager.i.verifyNativeColonies(sector);
          watchedSectors.add(sector);
          stars.addAll(sector.get().stars);
        }
      }
    }

    connection.send(new Packet.Builder()
        .star_updated(new StarUpdatedPacket.Builder()
            .stars(stars)
            .build())
        .build());

    synchronized (watchedStars) {
      for (Star star : stars) {
        WatchableObject<Star> watchableStar = StarManager.i.getStar(star.id);
        if (watchableStar == null) {
          // Huh?
          log.warning("Got unexpected null star: %d", star.id);
          continue;
        }
        watchableStar.addWatcher(starWatcher);
        watchedStars.put(star.id, watchableStar);
      }
    }
  }

  private void onModifyStar(ModifyStarPacket pkt) {
    WatchableObject<Star> star = StarManager.i.getStar(pkt.star_id);
    for (StarModification modification : pkt.modification) {
      if (modification.empire_id == null || !modification.empire_id.equals(empire.get().id)) {
        // Update the modification's empire_id to be our own, since that's what'll be recorded
        // in the database and we don't want this suspicious event to be recorded against the
        // other person's empire.
        Long otherEmpireId = modification.empire_id;
        modification = modification.newBuilder().empire_id(empire.get().id).build();

        SuspiciousEventManager.i.addSuspiciousEvent(new SuspiciousModificationException(
            pkt.star_id,
            modification,
            "Modification empire_id does not equal our own empire. empire_id=%d",
            otherEmpireId));
        return;
      }

      if (modification.full_fuel != null && modification.full_fuel) {
        // Clients shouldn't be trying to create fleets at all, but they should also not be trying
        // fill them with fuel. That's suspicious.
        SuspiciousEventManager.i.addSuspiciousEvent(new SuspiciousModificationException(
            pkt.star_id, modification, "Modification tried to set full_fuel to true."));
        return;
      }
    }

    try {
      StarManager.i.modifyStar(star, pkt.modification, null /* logHandler */);
    } catch (SuspiciousModificationException e) {
      SuspiciousEventManager.i.addSuspiciousEvent(e);
      log.warning("Suspicious star modification.", e);
    }
  }

  private void onRequestEmpire(RequestEmpirePacket pkt) {
    List<Empire> empires = new ArrayList<>();
    for (long id : pkt.empire_id) {
      WatchableObject<Empire> empire = EmpireManager.i.getEmpire(id);
      if (empire != null) {
        empires.add(empire.get());
      }
    }
    connection.send(new Packet.Builder()
        .empire_details(new EmpireDetailsPacket.Builder()
            .empires(empires)
            .build())
        .build());
  }

  private void onChatMessages(ChatMessagesPacket pkt) {
    if (pkt.messages.size() != 1) {
      // TODO: suspicious, should be only one chat message.
      log.error("Didn't get the expected 1 chat message. Got %d.", pkt.messages.size());
      return;
    }

    ChatManager.i.send(null /* TODO */, pkt.messages.get(0).newBuilder()
        .date_posted(System.currentTimeMillis())
        .empire_id(empire.get().id)
        .action(ChatMessage.MessageAction.Normal)
        .room_id(null /* TODO */)
        .build());
  }

  private final Participant.OnlineCallback chatCallback = new Participant.OnlineCallback() {
    @Override
    public void onChatMessage(List<ChatMessage> msgs) {
      connection.send(new Packet.Builder()
          .chat_msgs(new ChatMessagesPacket.Builder()
              .messages(msgs)
              .build())
          .build());
    }
  };

  private void clearWatchedStars() {
    synchronized (watchedStars) {
      for (WatchableObject<Star> star : watchedStars.values()) {
        star.removeWatcher(starWatcher);
      }
      watchedStars.clear();
    }
  }
}
