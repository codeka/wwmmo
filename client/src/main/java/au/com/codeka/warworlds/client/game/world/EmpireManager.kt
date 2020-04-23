package au.com.codeka.warworlds.client.game.world

import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.R
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.store.ProtobufStore
import au.com.codeka.warworlds.client.util.eventbus.EventHandler
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.EmpireDetailsPacket
import au.com.codeka.warworlds.common.proto.Packet
import au.com.codeka.warworlds.common.proto.RequestEmpirePacket
import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import java.util.*

/** Manages empires.  */
object EmpireManager {
  private val log = Log("EmpireManager")

  /**
   * The number of milliseconds to delay sending the empire request to the server, so that we can
   * batch them up a bit in case of a burst of requests (happens when you open chat for example).
   */
  private const val EMPIRE_REQUEST_DELAY_MS: Long = 150

  private val empires: ProtobufStore<Empire> = App.i.dataStore.empires()

  /** The list of pending empire requests.  */
  private val pendingEmpireRequests: MutableSet<Long> = HashSet()

  /** An object to synchronize on when updating [.pendingEmpireRequests].  */
  private val pendingRequestLock = Any()

  /** Whether a request for empires is currently pending.  */
  private var requestPending = false

  /** Our current empire, will be null before we're connected.  */
  private var myEmpire: Empire? = null

  /** A placeholder [Empire] for native empires.  */
  private val nativeEmpire = Empire.Builder()
      .display_name(App.i.getString(R.string.native_colony))
      .build()

  private val eventListener: Any = object : Any() {
    @EventHandler(thread = Threads.BACKGROUND)
    fun handleEmpireUpdatedPacket(pkt: EmpireDetailsPacket) {
      for (empire in pkt.empires) {
        val startTime = System.nanoTime()
        empires.put(empire.id, empire)
        App.i.eventBus.publish(empire)
        val endTime = System.nanoTime()
        log.debug("Refreshed empire %d [%s] in %dms.",
            empire.id, empire.display_name, (endTime - startTime) / 1000000L)
      }
    }
  }

  init {
    App.i.eventBus.register(eventListener)
  }

  /** Called by the server when we get the 'hello', and lets us know the empire.  */
  fun onHello(empire: Empire) {
    empires.put(empire.id, empire)
    myEmpire = empire
    App.i.eventBus.publish(empire)
  }

  /** Returns [true] if my empire has been set, or false if it's not ready yet.  */
  fun hasMyEmpire(): Boolean {
    return myEmpire != null
  }

  /** Gets my empire, if my empire hasn't been set yet, IllegalStateException is thrown.  */
  fun getMyEmpire(): Empire {
    return myEmpire!!
  }

  /** @return true if the given empire is mine.
   */
  fun isMyEmpire(empire: Empire?): Boolean {
    return if (empire == null || empire.id == null) {
      false
    } else empire.id == getMyEmpire().id
  }

  /** @return true if the given empire is an enemy of us.
   */
  fun isEnemy(empire: Empire?): Boolean {
    if (empire == null) {
      return false
    }
    if (empire.id == null) {
      return true
    }
    return if (myEmpire != null && empire.id != myEmpire!!.id) {
      true
    } else false
  }

  /**
   * Gets the [Empire] with the given id. If the id is null, returns a pseudo-Empire that
   * can be used for native colonies/fleets.
   */
  fun getEmpire(id: Long?): Empire? {
    if (id == null) {
      return nativeEmpire
    }
    if (myEmpire != null && myEmpire!!.id == id) {
      return myEmpire
    }
    val empire = empires[id]
    if (empire == null) {
      requestEmpire(id)
    }
    return empire
  }

  /**
   * Request the [Empire] with the given ID from the server. To save a storm of requests when
   * showing the chat screen (and others), we delay sending the request by a couple hundred
   * milliseconds.
   */
  private fun requestEmpire(id: Long) {
    synchronized(pendingRequestLock) {
      pendingEmpireRequests.add(id)
      if (!requestPending) {
        App.i.taskRunner.runTask(Runnable { sendPendingEmpireRequests() },
            Threads.BACKGROUND,
            EMPIRE_REQUEST_DELAY_MS)
      }
    }
  }

  /** Called on a background thread to actually send the request empire request to the server.  */
  private fun sendPendingEmpireRequests() {
    var empireIds: List<Long>?
    synchronized(pendingRequestLock) {
      empireIds = Lists.newArrayList(pendingEmpireRequests)
      pendingEmpireRequests.clear()
      requestPending = false
    }
    App.i.server.send(Packet.Builder()
        .request_empire(RequestEmpirePacket.Builder()
            .empire_id(empireIds)
            .build())
        .build())
  }
}