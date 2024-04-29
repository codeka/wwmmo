package au.com.codeka.warworlds.client.game.build

import android.graphics.Color
import au.com.codeka.warworlds.client.App
import au.com.codeka.warworlds.client.concurrency.Threads
import au.com.codeka.warworlds.client.game.world.EmpireManager
import au.com.codeka.warworlds.common.Log
import au.com.codeka.warworlds.common.proto.*
import au.com.codeka.warworlds.common.sim.BuildHelper
import au.com.codeka.warworlds.common.sim.MutableStar
import au.com.codeka.warworlds.common.sim.StarModifier
import au.com.codeka.warworlds.common.sim.SuspiciousModificationException
import com.google.common.collect.Lists;
import java.util.*
import kotlin.math.abs

typealias BuildTimeCalculatorCallback = (buildTime: String?, buildMinerals: String?, mineralsColor: Int) -> Unit

class BuildTimeCalculator(private val star: Star, private val colony: Colony) {

  fun calculateBuildTime(design: Design?, count: Int, callback: BuildTimeCalculatorCallback) {
    calculateTime(design, null, count, callback)
  }

  fun calculateUpgradeTime(design: Design?, building: Building?, callback: BuildTimeCalculatorCallback) {
    calculateTime(design, building, 1, callback)
  }

  private fun calculateTime(
      design: Design?, building: Building?, count: Int, callback: BuildTimeCalculatorCallback) {
    App.taskRunner.runOn(Threads.BACKGROUND) {
      // Add the build request to a temporary copy of the star, simulate it and figure out the
      // build time.
      val mutableStar = MutableStar.from(star)
      val myEmpire = EmpireManager.getMyEmpire()
      try {
        StarModifier { 0 }.modifyStar(mutableStar,
            StarModification(
                type = StarModification.Type.ADD_BUILD_REQUEST,
                empire_id = myEmpire.id,
                colony_id = colony.id,
                count = count,
                building_id = building?.id,
                design_type = design!!.type))
      } catch (e: SuspiciousModificationException) {
        log.error("Suspicious modification?", e)
        return@runOn
      }
      // find the build request with ID 0, that's our guy
      for (buildRequest in BuildHelper.getBuildRequests(mutableStar)) {
        if (buildRequest.id == 0L) {
          App.taskRunner.runOn(Threads.UI) {
            val buildTime = BuildHelper.formatTimeRemaining(buildRequest.build())
            val mineralsTime: String
            val mineralsColor: Int
            val newEmpireStorage = mutableStar.empireStores.find { it.empireId == myEmpire.id }
            val oldEmpireStorage = star.empire_stores.find { it.empire_id == myEmpire.id }
            if (newEmpireStorage != null && oldEmpireStorage != null) {
              val mineralsDelta =
                (newEmpireStorage.mineralsDeltaPerHour - oldEmpireStorage.minerals_delta_per_hour!!)
              mineralsTime = String.format(Locale.US, "%s%.1f/hr",
                  if (mineralsDelta < 0) "-" else "+", abs(mineralsDelta))
              mineralsColor = if (mineralsDelta < 0) Color.RED else Color.GREEN
            } else {
              mineralsTime = ""
              mineralsColor = Color.WHITE
            }
            callback(buildTime, mineralsTime, mineralsColor)
          }
          break
        }
      }
    }
  }

  companion object {
    private val log = Log("BuildTimeCalculator")
  }

}