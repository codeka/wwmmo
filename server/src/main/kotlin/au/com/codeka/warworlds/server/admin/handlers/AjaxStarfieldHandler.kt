package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.SectorCoord
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.proto.StarModification
import au.com.codeka.warworlds.common.sim.MutableStar
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
      "search" -> {
        val starId = request.getParameter("star_id").toLong()
        handleSearchRequest(starId)
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
      "resetSector" -> {
        val x = request.getParameter("x").toLong()
        val y = request.getParameter("y").toLong()
        handleResetSector(x, y)
      }
      else -> throw RequestException(400, "Unknown action: ${request.getParameter("action")}")
    }
  }

  private fun handleXyRequest(x: Long, y: Long) {
    val sector = SectorManager.i.getSector(SectorCoord(x = x, y = y))
    SectorManager.i.verifyNativeColonies(sector)
    setResponseJson(sector.get())
  }

  private fun handleSearchRequest(starId: Long) {
    val star = StarManager.i.getStar(starId)
    if (star == null) {
      response.status = 404
      return
    }
    setResponseJson(star.get())
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
    modifyAndSimulate(starId, StarModification(
        type = StarModification.Type.EMPTY_NATIVE))
  }

  private fun handleForceMoveComplete(starId: Long, fleetId: Long) {
    log.debug("force move complete (star: %d, fleet: %d)", starId, fleetId)
    val starWo: WatchableObject<Star> = StarManager.i.getStar(starId) ?: return
    synchronized(starWo.lock) {
      var star = starWo.get()
      for (i in star.fleets.indices) {
        if (star.fleets[i].id == fleetId) {
          val fleets = ArrayList(star.fleets)
          fleets[i] = fleets[i].copy(eta = 100L)
          star = star.copy(fleets = fleets)
        }
      }
      starWo.set(star)
    }

    // Now just simulate to make sure it processes it.
    setResponseGson(modifyAndSimulate(starId, null))
  }

  private fun handleForceBuildRequestComplete(starId: Long, buildRequestId: Long) {
    log.debug("force build request complete (star: %d, req: %d)", starId, buildRequestId)
    val starWo: WatchableObject<Star> = StarManager.i.getStar(starId) ?: return
    synchronized(starWo.lock) {
      val star = MutableStar.from(starWo.get())
      for (planet in star.planets) {
        val colony = planet.colony ?: continue
        for (br in colony.buildRequests) {
          if (br.id == buildRequestId) {
            // Set the end time well in the past, so that the star manager think it's done.
            br.endTime = 100L
            br.progress = 1.0f
          }
        }
      }
      starWo.set(star.build())
    }

    // Now just simulate to make sure it processes it.
    setResponseGson(modifyAndSimulate(starId, null))
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

  private fun handleResetSector(x: Long, y: Long) {
    SectorManager.i.resetSector(SectorCoord(x = x, y = y))
  }

  private class LogHandler constructor(private val logMessages: StringBuilder)
      : Simulation.LogHandler {
    override fun setStarName(starName: String?) {
      // ignore.
    }

    override fun error(message: String, vararg args: Any?) {
      logMessages.append(String.format(message, args))
      logMessages.append("\n")
    }

    override fun log(message: String, vararg args: Any?) {
      logMessages.append(String.format(message, args))
      logMessages.append("\n")
    }
  }

  private class SimulateResponse {
    var loadTime: Long = 0
    var simulateTime: Long = 0
    var logMessages: String? = null
  }
}