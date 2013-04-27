package au.com.codeka.warworlds.server.designeffects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.ShipDesign;
import au.com.codeka.common.model.ShipEffect;
import au.com.codeka.warworlds.server.model.DesignManager;
import au.com.codeka.warworlds.server.model.Fleet;

/**
 * This effect is attached to ships of the kind "fighter". It makes the ship participate in
 * combat.
 */
public class FighterShipEffect extends ShipEffect {
    private final Logger log = LoggerFactory.getLogger(FighterShipEffect.class);

    /**
     * This is called when we arrive on a star. If there's anybody to attack, we'll switch to
     * an attacking state.
     */
    @Override
    public void onArrived(BaseStar star, BaseFleet fleet) {
        for (BaseFleet existingBaseFleet : star.getFleets()) {
            Fleet existingFleet = (Fleet) existingBaseFleet;
            if (existingFleet.getID() == ((Fleet) fleet).getID()) {
                continue;
            }

            // if it's the same empire, then we're not going to attack it
            if (existingFleet.getEmpireID() == ((Fleet) fleet).getEmpireID()) {
                // TODO: also check alliances...
                continue;
            }

            // if it's not a fighter, then it's not combat-worthy
            ShipDesign existingFleetShipDesign = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, existingFleet.getDesignID());
            if (!existingFleetShipDesign.hasEffect(FighterShipEffect.class)) {
                continue;
            }

            log.info(String.format("Fleet #%s arrived at star #%s, found enemy fleet, switching to attack mode.",
                                   fleet.getKey(), star.getKey()));
            ((Fleet) fleet).attack();
            break;
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

        // if the other fleet is the same empire, that's fine
        if (((Fleet) fleet).getEmpireID() == ((Fleet) otherFleet).getEmpireID()) {
            return;
        }

        // if it's not a fighter, then it's not combat-worthy
        ShipDesign otherFleetShipDesign = (ShipDesign) DesignManager.i.getDesign(DesignKind.SHIP, otherFleet.getDesignID());
        if (!otherFleetShipDesign.hasEffect(FighterShipEffect.class)) {
            return;
        }

        log.info(String.format("Fleet #%s arrived at star #%s, switching to attack mode.",
                otherFleet.getKey(), star.getKey()));
        ((Fleet) fleet).attack();
    }


    /**
     * This is called if we're idle and someone attacks us.
     */
    public void onAttacked(BaseStar star, BaseFleet fleet, BaseFleet otherFleet) {
        log.info(String.format("Fleet #%d has been attacked, switching to attack mode."));
        ((Fleet) fleet).attack();
    }
}
