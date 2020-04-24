package au.com.codeka.warworlds.server.admin.handlers

import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.Log.LogHook
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.SectorCoord
import au.com.codeka.warworlds.server.handlers.RequestException
import au.com.codeka.warworlds.server.store.DataStore
import au.com.codeka.warworlds.server.store.SectorsStore.SectorState
import au.com.codeka.warworlds.server.world.EmpireManager
import au.com.codeka.warworlds.server.world.WatchableObject
import au.com.codeka.warworlds.server.world.generator.NewStarFinder
import java.util.*

/** Handler for /admin/ajax/sectors requests.  */
class AjaxSectorsHandler : AjaxHandler() {
  @Throws(RequestException::class)
  public override fun get() {
    when (request.getParameter("action")) {
      "find-empty" -> handleFindEmptyRequest()
      "create-empire" -> handleCreateEmpireRequest()
      "expand" -> handleExpandRequest()
      else -> throw RequestException(400, "Unknown action: " + request.getParameter("action"))
    }
  }

  private fun handleFindEmptyRequest() {
    setResponseJson(DataStore.i.sectors().findSectorByState(SectorState.Empty))
  }

  private fun handleCreateEmpireRequest() {
    val name = request.getParameter("name")
    val xs = request.getParameter("x")
    val ys = request.getParameter("y")
    val resp = CreateEmpireResponse()
    resp.empireName = name
    var coord: SectorCoord? = null
    if (xs != null && ys != null) {
      coord = SectorCoord.Builder().x(xs.toLong()).y(ys.toLong()).build()
      resp.sectorX = coord.x
      resp.sectorY = coord.y
    }
    val newStarFinder = NewStarFinder(Log(object : LogHook {
      override fun write(msg: String?) {
        resp.log(msg)
      }
    }), coord)
    if (!newStarFinder.findStarForNewEmpire()) {
      resp.log("No star found.")
      setResponseGson(resp)
      return
    }
    resp.sectorX = newStarFinder.star.sector_x
    resp.sectorY = newStarFinder.star.sector_y
    val empire = EmpireManager.i.createEmpire(name, newStarFinder)
    if (empire == null) {
      resp.log("Failed to create empire.")
    } else {
      resp.empire = empire.get()
    }
    setResponseGson(resp)
  }

  private fun handleExpandRequest() {
    DataStore.Companion.i.sectors().expandUniverse()
  }

  /** Class that's sent to the client via Gson-encoder.  */
  private class CreateEmpireResponse {
    var empireName: String? = null
    var sectorX: Long = 0
    var sectorY: Long = 0
    var logs: MutableList<String?> = ArrayList()
    var empire: Empire? = null
    fun log(msg: String?) {
      logs.add(msg)
    }
  }

  companion object {
    private val log = Log("AjaxSectorsHandler")
  }
}