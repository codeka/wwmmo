package au.com.codeka.warworlds.client.game.build

import android.graphics.Color
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.sim.BuildHelper
import au.com.codeka.warworlds.common.sim.StarModifier
import au.com.codeka.warworlds.common.sim.StarModifier.IdentifierGenerator
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException
import java.util.*

typealias BuildTimeCalculatorCallback = (buildTime: String?, buildMinerals: String?, mineralsColor: Int) -> Unit

class BuildTimeCalculator(private val star: Star?, private val colony: Colony?) {

  fun calculateBuildTime(design: Design?, count: Int, callback: BuildTimeCalculatorCallback) {
    calculateTime(design, null, count, callback)
  }

  fun calculateUpgradeTime(design: Design?, building: Building?, callback: BuildTimeCalculatorCallback) {
    calculateTime(design, building, 1, callback)
  }

  private fun calculateTime(
      design: Design?, building: Building?, count: Int, callback: BuildTimeCalculatorCallback) {
    App.i.taskRunner.runTask(Runnable {
      // Add the build request to a temporary copy of the star, simulate it and figure out the
      // build time.
      val starBuilder = star!!.newBuilder()
      val myEmpire = EmpireManager.i.myEmpire
      try {
        StarModifier(IdentifierGenerator { 0 }).modifyStar(starBuilder,
            StarModification.Builder()
                .type(StarModification.MODIFICATION_TYPE.ADD_BUILD_REQUEST)
                .empire_id(myEmpire.id)
                .colony_id(colony!!.id)
                .count(count)
                .building_id(building?.id)
                .design_type(design!!.type)
                .build())
      } catch (e: SuspiciousModificationException) {
        log.error("Suspicious modification?", e)
        return@Runnable
      }
      // find the build request with ID 0, that's our guy
      val updatedStar = starBuilder.build()
      for (buildRequest in BuildHelper.getBuildRequests(updatedStar)) {
        if (buildRequest.id == 0L) {
          App.i.taskRunner.runTask(Runnable {
            val buildTime = BuildHelper.formatTimeRemaining(buildRequest)
            val mineralsTime: String
            val mineralsColor: Int
            val newEmpireStorage = BuildHelper.getEmpireStorage(updatedStar, myEmpire.id)
            val oldEmpireStorage = BuildHelper.getEmpireStorage(star, myEmpire.id)
            if (newEmpireStorage != null && oldEmpireStorage != null) {
              val mineralsDelta = (newEmpireStorage.minerals_delta_per_hour
                  - oldEmpireStorage.minerals_delta_per_hour)
              mineralsTime = String.format(Locale.US, "%s%.1f/hr",
                  if (mineralsDelta < 0) "-" else "+", Math.abs(mineralsDelta))
              mineralsColor = if (mineralsDelta < 0) Color.RED else Color.GREEN
            } else {
              mineralsTime = ""
              mineralsColor = Color.WHITE
            }
            callback(buildTime, mineralsTime, mineralsColor)
          }, Threads.UI)
          break
        }
      }
    }, Threads.BACKGROUND)
  }

  companion object {
    private val log = Log("BuildTimeCalculator")
  }

}