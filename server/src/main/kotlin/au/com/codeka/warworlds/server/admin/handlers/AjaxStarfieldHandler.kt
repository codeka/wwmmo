package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.SectorCoord
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.proto.StarModification
import au.com.codeka.warworlds.common.sim.Simulation
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.world.SectorManager
import au.com.codeka.warworlds.server.world.StarManager
import au.com.codeka.warworlds.server.world.SuspiciousEventManager
import au.com.codeka.warworlds.server.world.WatchableObject
import java.util.*

/** Handler for /admin/ajax/starfield requests.  */
class AjaxStarfieldHandler : AjaxHandler() {
  companion object {
    private val log = Log("AjaxStarfieldHandler")
  }

  public override fun get() {
    when (request.getParameter("action")) {
      "xy" -> {
        val x = request.getParameter("x").toLong()
        val y = request.getParameter("y").toLong()
        handleXyRequest(x, y)
      }
      else -> throw RequestException(400, "Unknown action: ${request.getParameter("action")}")
    }
  }

  public override fun post() {
    when (request.getParameter("action")) {
      "simulate" -> {
        val starId = request.getParameter("id").toLong()
        handleSimulateRequest(starId)
      }
      "modify" -> {
        val starId = request.getParameter("id").toLong()
        val modifyJson = request.getParameter("modify")
        handleModifyRequest(starId, modifyJson)
      }
      "delete" -> {
        val starId = request.getParameter("id").toLong()
        handleDeleteRequest(starId)
      }
      "clearNatives" -> {
        val starId = request.getParameter("id").toLong()
        handleClearNativesRequest(starId)
      }
      "forceMoveComplete" -> {
        val starId = request.getParameter("id").toLong()
        val fleetId = request.getParameter("fleetId").toLong()
        handleForceMoveComplete(starId, fleetId)
      }
      "forceBuildRequestComplete" -> {
        val starId = request.getParameter("id").toLong()
        val buildRequestId = request.getParameter("reqId").toLong()
        handleForceBuildRequestComplete(starId, buildRequestId)
      }
      else -> throw RequestException(400, "Unknown action: ${request.getParameter("action")}")
    }
  }

  private fun handleXyRequest(x: Long, y: Long) {
    val sector = SectorManager.i.getSector(SectorCoord.Builder().x(x).y(y).build())
    SectorManager.i.verifyNativeColonies(sector)
    setResponseJson(sector.get())
  }

  private fun handleSimulateRequest(starId: Long) {
    setResponseGson(modifyAndSimulate(starId, null))
  }

  private fun handleModifyRequest(starId: Long, modifyJson: String) {
    val modification = fromJson(modifyJson, StarModification::class.java)
    setResponseGson(modifyAndSimulate(starId, modification))
  }

  private fun handleDeleteRequest(starId: Long) {
    log.debug("delete star: %d", starId)
    StarManager.i.deleteStar(starId)
  }

  private fun handleClearNativesRequest(starId: Long) {
    log.debug("delete star: %d", starId)
    modifyAndSimulate(starId, StarModification.Builder()
        .type(StarModification.MODIFICATION_TYPE.EMPTY_NATIVE)
        .build())
  }

  private fun handleForceMoveComplete(starId: Long, fleetId: Long) {
    log.debug("force move complete (star: %d, fleet: %d)", starId, fleetId)
    val starWo: WatchableObject<Star> = StarManager.i.getStar(starId) ?: return
    synchronized(starWo.lock) {
      val star = starWo.get().newBuilder()
      for (i in star.fleets.indices) {
        if (star.fleets[i].id == fleetId) {
          // Set the ETA well in the past, so that the star manager thinks it should have arrived.
          star.fleets[i] = star.fleets[i].newBuilder()
              .eta(100L)
              .build()
        }
      }
      starWo.set(star.build())
    }

    // Now just simulate to make sure it processes it.
    modifyAndSimulate(starId, null)
  }

  private fun handleForceBuildRequestComplete(starId: Long, buildRequestId: Long) {
    log.debug("force build request complete (star: %d, req: %d)", starId, buildRequestId)
    val starWo: WatchableObject<Star> = StarManager.i.getStar(starId) ?: return
    synchronized(starWo.lock) {
      val star = starWo.get().newBuilder()
      for (i in star.planets.indices) {
        val planet = star.planets[i]
        if (planet.colony == null) {
          continue
        }
        for (j in planet.colony.build_requests.indices) {
          val buildRequest = planet.colony.build_requests[j]
          if (buildRequest.id == buildRequestId) {
            // Set the end time well in the past, so that the star manager think it's done.
            val colonyBuilder = planet.colony.newBuilder()
            colonyBuilder.build_requests[j] = buildRequest.newBuilder()
                .end_time(100L)
                .progress(1.0f)
                .build()
            star.planets[i] = planet.newBuilder()
                .colony(colonyBuilder.build())
                .build()
          }
        }
      }
      starWo.set(star.build())
    }

    // Now just simulate to make sure it processes it.
    modifyAndSimulate(starId, null)
  }

  private fun modifyAndSimulate(starId: Long, modification: StarModification?): SimulateResponse {
    val resp = SimulateResponse()
    val startTime = System.nanoTime()
    val star = StarManager.i.getStarOrError(starId)
    resp.loadTime = (System.nanoTime() - startTime) / 1000000L
    val logMessages = StringBuilder()
    val modifications = ArrayList<StarModification>()
    if (modification != null) {
      modifications.add(modification)
    }

    try {
      StarManager.i.modifyStar(star, modifications, LogHandler(logMessages))
    } catch (e: SuspiciousModificationException) {
      log.warning("Suspicious modification.", e)
      // We'll log it as well, even though technically it wasn't the empire who made it.
      SuspiciousEventManager.i.addSuspiciousEvent(e)
      throw RequestException(e)
    }

    val simulateTime = System.nanoTime()
    resp.simulateTime = (simulateTime - startTime) / 1000000L
    resp.logMessages = logMessages.toString()
    return resp
  }

  private class LogHandler internal constructor(private val logMessages: StringBuilder)
      : Simulation.LogHandler {
    override fun setStarName(starName: String?) {
      // ignore.
    }

    override fun log(message: String) {
      logMessages.append(message)
      logMessages.append("\n")
    }
  }

  private class SimulateResponse {
    var loadTime: Long = 0
    var simulateTime: Long = 0
    var logMessages: String? = null
  }
}