package au.com.codeka.warworlds.game;

import au.com.codeka.warworlds.model.Empire;

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

    public Empire getEmpire() {
        return mEmpire;
    }
}
