package au.com.codeka.common.design;

import au.com.codeka.common.model.Fleet;
import au.com.codeka.common.model.Star;

/**
 * This is the base \see DesignEffect for ships.
 */
public class ShipEffect extends Design.Effect {
    /**
     * This is called when we arrive on a star. If there's anybody to attack, we'll switch to
     * an attacking state.
     */
    public void onArrived(Star star, Fleet fleet) {
    }

    /**
     * This is called when we're already on a star, but *another* fleet arrives. We might want
     * to attack it (or something).
     */
    public void onOtherArrived(Star star, Fleet fleet, Fleet otherFleet) {
    }

    /**
     * This is called if we're idle and someone attacks us.
     */
    public void onAttacked(Star star, Fleet fleet) {
    }
}
