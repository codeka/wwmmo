package au.com.codeka.warworlds.server.designeffects;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseFleet.Stance;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.model.ShipEffect;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.warworlds.server.model.DesignManager;
import au.com.codeka.warworlds.server.model.Fleet;

/**
 * This effect is attached to ships of the kind "fighter". It makes the ship participate in
 * combat.
 */
public class FighterShipEffect extends ShipEffect {
  private final Log log = new Log("FighterShipEffect");

  /**
   * This is called when we arrive on a star. If there's anybody to attack, we'll switch to an
   * attacking state.
   */
  @Override
  public void onArrived(BaseStar star, BaseFleet fleet) {
    for (BaseFleet existingBaseFleet : star.getFleets()) {
      Fleet existingFleet = (Fleet) existingBaseFleet;
      if (existingFleet.getID() == ((Fleet) fleet).getID()) {
        continue;
      }

      // If it's already doing something, skip it.
      if (existingFleet.getState() != BaseFleet.State.IDLE) {
        continue;
      }

      // if it's friendly, then we're not going to attack it
      if (Simulation.isFriendly(existingFleet, fleet)) {
        continue;
      }

      // if it's not a fighter, then it's not combat-worthy
      ShipDesign existingFleetShipDesign =
          (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, existingFleet.getDesignID());
      if (!existingFleetShipDesign.hasEffect(FighterShipEffect.class)) {
        continue;
      }

      // if the existing fleet has a cloak and is not aggressive, it's not combat-worthy
      if (existingFleet.hasUpgrade("cloak") && existingFleet.getStance() != Stance.AGGRESSIVE) {
        continue;
      }

      // Either us or the other fleet needs to be AGGRESSIVE to switch to attack mode.
      if (fleet.getState() != BaseFleet.State.ATTACKING && fleet.getStance() == Stance.AGGRESSIVE) {
        log.info("Fleet #%s arrived at star #%s, found enemy fleet, switching to attack mode.",
            fleet.getKey(), star.getKey());
        fleet.attack(DateTime.now());
      }

      if (existingFleet.getState() != BaseFleet.State.ATTACKING
          && existingFleet.getStance() == Stance.AGGRESSIVE) {
        log.info("Fleet #%s arrived at star #%s, an enemy fleet is switching to attack mode.",
            fleet.getKey(), star.getKey());
        existingFleet.attack(DateTime.now());
      }
    }
  }

  /**
   * This is called when we're already on a star, but *another* fleet arrives. We might want
   * to attack it (or something).
   */
  @Override
  public void onOtherArrived(BaseStar star, BaseFleet fleet, BaseFleet otherFleet) {
    if (fleet.getState() != Fleet.State.IDLE) {
      return;
    }
    if (fleet.getStance() != Fleet.Stance.AGGRESSIVE) {
      return;
    }

    // if the other fleet is the same empire, that's fine
    if (Simulation.isFriendly(fleet, otherFleet)) {
      return;
    }

    // if we has a cloaking device, and it's not AGGRESSIVE, then we can't see it
    if (((Fleet) otherFleet).hasUpgrade("cloak") && otherFleet.getStance() != Stance.AGGRESSIVE) {
      return;
    }

    // if it's not a fighter, then it's not combat-worthy
    ShipDesign otherFleetShipDesign = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, otherFleet.getDesignID());
    if (!otherFleetShipDesign.hasEffect(FighterShipEffect.class)) {
      return;
    }

    log.info("Fleet #%s arrived at star #%s, #%s switching to attack mode.",
        otherFleet.getKey(), star.getKey(), fleet.getKey());
    ((Fleet) fleet).attack(DateTime.now());
  }

  /**
   * This is called if we're idle and someone attacks us.
   */
  @Override
  public void onAttacked(BaseStar star, BaseFleet fleet) {
    log.info(String.format("Fleet #%s has been attacked, switching to attack mode.", fleet.getKey()));
    ((Fleet) fleet).attack(DateTime.now());
  }
}
