package au.com.codeka.warworlds.game;

import warworlds.Warworlds.Empire;

/**
 * Manages stuff about your empire (e.g. colonizing planets and whatnot).
 */
public class EmpireManager {
    private static EmpireManager sInstance = new EmpireManager();

    public static EmpireManager getInstance() {
        return sInstance;
    }

    private Empire mEmpire;

    /**
     * This is called when you first connect to the server. We need to pass in details about
     * the empire and stuff.
     */
    public void setup(Empire empire) {
        mEmpire = empire;
    }

    /**
     * Gets the display name of this empire.
     */
    public String getDisplayName() {
        return mEmpire.getDisplayName();
    }

    /**
     * Gets the identifier of this empire, which is a unique ID you can use to reference
     * it other places.
     */
    public String getID() {
        return mEmpire.getId();
    }
}
