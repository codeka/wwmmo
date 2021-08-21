package au.com.codeka.warworlds.common.sim

import au.com.codeka.warworlds.common.proto.Design
import au.com.codeka.warworlds.common.proto.Empire
import au.com.codeka.warworlds.common.proto.Fleet
import au.com.codeka.warworlds.common.proto.Star
import au.com.codeka.warworlds.common.sim.DesignHelper.getDesign

/** Some helper methods for working with fleets. */
object FleetHelper {
  /* Is the given {@link Fleet} friendly to the given {@link Empire}? */
  fun isFriendly(fleet: Fleet, empire: Empire): Boolean {
    return isFriendly(fleet, empire.id)
  }

  /* Is the given {@link Fleet} friendly to the given {@link Empire}? */
  fun isFriendly(fleet: Fleet, empireId: Long?): Boolean {
    if (fleet.empire_id == null && empireId == null) {
      // Natives are friendly to each other.
      return true
    }
    return if (fleet.empire_id == null || empireId == null) {
      // Natives are not friendly to anybody else.
      false
    } else fleet.empire_id == empireId
    // TODO: alliance?
  }

  fun isFriendly(fleet1: Fleet, fleet2: Fleet): Boolean {
    return isFriendly(fleet1, fleet2.empire_id)
  }

  fun isOwnedBy(fleet: Fleet, empire: Empire?): Boolean {
    return isOwnedBy(fleet, empire?.id)
  }

  fun isOwnedBy(fleet: Fleet, empireId: Long?): Boolean {
    if (fleet.empire_id == null && empireId == null) {
      return true
    }
    return if (fleet.empire_id == null || empireId == null) {
      false
    } else fleet.empire_id == empireId
  }

  fun findFleet(star: Star, fleetId: Long): Fleet? {
    for (fleet in star.fleets) {
      if (fleet.id == fleetId) {
        return fleet
      }
    }
    return null
  }

  fun hasEffect(fleet: Fleet, effectType: Design.EffectType): Boolean {
    val design = getDesign(fleet.design_type)
    for (effect in design.effect) {
      if (effect.type == effectType) {
        return true
      }
    }
    return false
  }

//  fun hasUpgrade(fleet: Fleet?, upgradeType: Design.UpgradeType?): Boolean {
    // TODO: implement upgrades
//    return false
//  }
}
