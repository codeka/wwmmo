package au.com.codeka.warworlds.server.world

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.debug.PacketDebug
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.proto.Star.CLASSIFICATION
import au.com.codeka.warworlds.common.sim.EmpireHelper
import au.com.codeka.warworlds.common.sim.FleetHelper
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException
import au.com.codeka.warworlds.server.concurrency.TaskRunner
import au.com.codeka.warworlds.server.concurrency.Threads
import au.com.codeka.warworlds.server.net.Connection
import au.com.codeka.warworlds.server.world.chat.ChatManager
import au.com.codeka.warworlds.server.world.chat.Participant.OnlineCallback
import com.google.common.collect.Lists
import java.util.*

/** Represents a currently-connected player.  */
class Player(private val connection: Connection,
             private val helloPacket: HelloPacket,
             private val empire: WatchableObject<Empire>) {
  /** The list of [Sector]s this player is currently watching.  */
  private val watchedSectors = ArrayList<WatchableObject<Sector>>()

  /** The [Star]s that we are currently watching.  */
  private val watchedStars: MutableMap<Long, WatchableObject<Star>> = HashMap()

  /** The [WatchableObject.Watcher] which we'll be watching stars with.  */
  private val starWatcher: WatchableObject.Watcher<Star>

  init {
    log.setPrefix(String.format(Locale.US, "[%d %s]", empire.get().id, empire.get().display_name))
    starWatcher = object : WatchableObject.Watcher<Star> {
      override fun onUpdate(obj: WatchableObject<Star>) {
        sendStarsUpdatedPacket(Lists.newArrayList(obj.get()))
      }
    }
    TaskRunner.i.runTask(Runnable { onPostConnect() }, Threads.BACKGROUND)
  }

  fun onPacket(pkt: Packet) {
    if (pkt.watch_sectors != null) {
      onWatchSectorsPacket(pkt.watch_sectors)
    } else if (pkt.modify_star != null) {
      onModifyStar(pkt.modify_star)
    } else if (pkt.request_empire != null) {
      onRequestEmpire(pkt.request_empire)
    } else if (pkt.chat_msgs != null) {
      onChatMessages(pkt.chat_msgs)
    } else {
      log.error("Unknown/unexpected packet. %s", PacketDebug.getPacketDebug(pkt))
    }
  }

  /**
   * This is called on a background thread when this [Player] is created. We'll send the
   * client some updates they might be interested in.
   */
  private fun onPostConnect() {
    val startTime = System.nanoTime()
    val stars: ArrayList<WatchableObject<Star>?> = StarManager.i.getStarsForEmpire(empire.get().id)
    log.debug("Fetched %d stars for empire %d in %dms", stars.size, empire.get().id,
        (System.nanoTime() - startTime) / 1000000L)

    // Of the player's stars, send them all the ones that have been updated since their
    // last_simulation.
    val updatedStars = ArrayList<Star?>()
    for (star in stars) {
      if (helloPacket.our_star_last_simulation == null
          || (star!!.get().last_simulation != null
              && star.get().last_simulation > helloPacket.our_star_last_simulation)) {
        updatedStars.add(star!!.get())
      }
    }
    if (updatedStars.isNotEmpty()) {
      log.debug("%d updated stars, sending update packet.", updatedStars.size)
      sendStarsUpdatedPacket(updatedStars)
    } else {
      log.debug("No updated stars, not sending update packet.")
    }

    // Register this player with the chat system so that we get notified of messages.
    ChatManager.i.connectPlayer(empire.get().id, helloPacket.last_chat_time, chatCallback)
  }

  /**
   * Called when the client disconnects from us.
   */
  fun onDisconnect() {
    ChatManager.i.disconnectPlayer(empire.get().id)
    clearWatchedStars()
    synchronized(watchedSectors) { watchedSectors.clear() }
  }

  private fun onWatchSectorsPacket(pkt: WatchSectorsPacket) {
    // TODO: if we're already watching some of these sectors, we can just keep watching those,

    // Remove all our current watched stars
    clearWatchedStars()
    val stars: MutableList<Star?> = ArrayList()
    synchronized(watchedSectors) {
      watchedSectors.clear()
      for (sectorY in pkt.top..pkt.bottom) {
        for (sectorX in pkt.left..pkt.right) {
          val sector = SectorManager.i.getSector(SectorCoord.Builder().x(sectorX).y(sectorY).build())
          SectorManager.i.verifyNativeColonies(sector)
          watchedSectors.add(sector)
          stars.addAll(sector.get().stars)
        }
      }
    }
    sendStarsUpdatedPacket(stars)
    synchronized(watchedStars) {
      for (star in stars) {
        val watchableStar = StarManager.i.getStar(star!!.id)
        if (watchableStar == null) {
          // Huh?
          log.warning("Got unexpected null star: %d", star.id)
          continue
        }
        watchableStar.addWatcher(starWatcher)
        watchedStars[star.id] = watchableStar
      }
    }
  }

  private fun onModifyStar(pkt: ModifyStarPacket) {
    val star = StarManager.i.getStar(pkt.star_id)
    for (i in 0..pkt.modification.size) {
      var modification = pkt.modification[i]
      if (modification.empire_id == null || modification.empire_id != empire.get().id) {
        // Update the modification's empire_id to be our own, since that's what'll be recorded
        // in the database and we don't want this suspicious event to be recorded against the
        // other person's empire.
        val otherEmpireId = modification.empire_id
        modification = modification.newBuilder().empire_id(empire.get().id).build()
        SuspiciousEventManager.i.addSuspiciousEvent(SuspiciousModificationException(
            pkt.star_id,
            modification,
            "Modification empire_id does not equal our own empire. empire_id=%d",
            otherEmpireId))
        return
      }
      if (modification.full_fuel != null && modification.full_fuel) {
        // Clients shouldn't be trying to create fleets at all, but they should also not be trying
        // fill them with fuel. That's suspicious.
        SuspiciousEventManager.i.addSuspiciousEvent(SuspiciousModificationException(
            pkt.star_id, modification, "Modification tried to set full_fuel to true."))
        return
      }
    }
    try {
      StarManager.i.modifyStar(star, pkt.modification, null /* logHandler */)
    } catch (e: SuspiciousModificationException) {
      SuspiciousEventManager.i.addSuspiciousEvent(e)
      log.warning("Suspicious star modification.", e)
    }
  }

  private fun onRequestEmpire(pkt: RequestEmpirePacket) {
    val empires: MutableList<Empire?> = ArrayList()
    for (id in pkt.empire_id) {
      val empire: WatchableObject<Empire>? = EmpireManager.i.getEmpire(id)
      if (empire != null) {
        empires.add(empire.get())
      }
    }
    connection.send(Packet.Builder()
        .empire_details(EmpireDetailsPacket.Builder()
            .empires(empires)
            .build())
        .build())
  }

  private fun onChatMessages(pkt: ChatMessagesPacket) {
    if (pkt.messages.size != 1) {
      // TODO: suspicious, should be only one chat message.
      log.error("Didn't get the expected 1 chat message. Got %d.", pkt.messages.size)
      return
    }
    ChatManager.i.send(null /* TODO */, pkt.messages[0].newBuilder()
        .date_posted(System.currentTimeMillis())
        .empire_id(empire.get().id)
        .action(ChatMessage.MessageAction.Normal)
        .room_id(null /* TODO */)
        .build())
  }

  private val chatCallback = object : OnlineCallback {
    override fun onChatMessage(msgs: List<ChatMessage?>?) {
      connection.send(Packet.Builder()
          .chat_msgs(ChatMessagesPacket.Builder()
              .messages(msgs)
              .build())
          .build())
    }
  }

  /**
   * Send a [StarUpdatedPacket] with the given list of updated stars.
   *
   *
   * The most important function of this method is sanitizing the stars so that enemy fleets
   * are not visible (unless we have a colony/fleet as well, or there's a radar nearby).
   */
  private fun sendStarsUpdatedPacket(updatedStars: MutableList<Star?>) {
    for (i in updatedStars.indices) {
      updatedStars[i] = sanitizeStar(updatedStars[i])
    }
    connection.send(Packet.Builder()
        .star_updated(StarUpdatedPacket.Builder()
            .stars(updatedStars)
            .build())
        .build())
  }

  /**
   * Sanitizes the given star. Removes enemy fleets, etc, unless we have a colony or fleet of our
   * own on there, or there's a radar nearby.
   */
  // TODO: check for existence of radar buildings nearby
  private fun sanitizeStar(star: Star?): Star? {
    // If the star is a wormhole, don't sanitize it -- a wormhole is basically fleets in transit
    // anyway.
    if (star!!.classification == CLASSIFICATION.WORMHOLE) {
      return star
    }
    val myEmpireId = empire.get().id

    // Now, figure out if we need to sanitize this star at all. Full sanitization means we need
    // to remove all fleets and simplify colonies (no population, etc). Partial sanitization means
    // we need to remove some fleets that have the cloaking upgrade.
    var needFullSanitization = true
    var needPartialSanitization = false
    for (planet in star.planets) {
      if (planet.colony != null && planet.colony.empire_id != null && planet.colony.empire_id == myEmpireId) {
        // If we have a colony on here, then we get the full version of the star.
        needFullSanitization = false
        break
      }
    }
    for (fleet in star.fleets) {
      if (FleetHelper.isOwnedBy(fleet, myEmpireId)) {
        // If we have a fleet on here, then we also get the full version.
        needFullSanitization = false
      }
      if (FleetHelper.hasUpgrade(fleet, Design.UpgradeType.CLOAK)) {
        needPartialSanitization = true
      }
    }

    // If there's any non-us scout reports we'll need to do a partial sanitization.
    for (scoutReport in star.scout_reports) {
      if (scoutReport.empire_id != null && scoutReport.empire_id != myEmpireId) {
        needPartialSanitization = true
      }
    }

    // TODO: if we have a radar nearby, then we get the full version of the star.

    // If we need neither full nor partial sanitization, we can save a bunch of time.
    if (!needFullSanitization && !needPartialSanitization) {
      return star
    }

    // OK, now we know we need to sanitize this star.
    val starBuilder = star.newBuilder()

    // If need to do full sanitization, then do that.
    if (needFullSanitization) {
      run {
        var i = 0
        while (i < starBuilder.fleets.size) {

          // Only remove if non-moving. TODO: also remove moving fleets if there's no radar nearby
          if (starBuilder.fleets[i].state != Fleet.FLEET_STATE.MOVING) {
            starBuilder.fleets.removeAt(i)
            i--
          }
          i++
        }
      }
      for (i in starBuilder.planets.indices) {
        // Sanitize colonies. We can see that they exist, but we only get certain details.
        if (starBuilder.planets[i].colony != null) {
          val colony = starBuilder.planets[i].colony.newBuilder()
              .population(0f)
              .build_requests(ArrayList())
              .buildings(ArrayList())
              .defence_bonus(0f)
              .delta_energy(0f)
              .delta_goods(0f)
              .delta_minerals(0f)
              .delta_population(0f)
              .build()
          starBuilder.planets[i] = starBuilder.planets[i].newBuilder().colony(colony).build()
        }
      }
    } else {
      // Even if we don't need full sanitization, we'll remove any fleets that have the cloaking
      // upgrade.
      // TODO: implement me
    }

    // Remove any scout reports that are not for us.
    var myScoutReport: ScoutReport? = null
    for (i in starBuilder.scout_reports.indices) {
      if (EmpireHelper.isSameEmpire(starBuilder.scout_reports[i].empire_id, myEmpireId)) {
        myScoutReport = starBuilder.scout_reports[i]
        break
      }
    }
    starBuilder.scout_reports.clear()
    if (myScoutReport != null) {
      starBuilder.scout_reports.add(myScoutReport)
    }
    return starBuilder.build()
  }

  private fun clearWatchedStars() {
    synchronized(watchedStars) {
      for (star in watchedStars.values) {
        star.removeWatcher(starWatcher)
      }
      watchedStars.clear()
    }
  }

  companion object {
    private val log = Log("Player")
  }
}