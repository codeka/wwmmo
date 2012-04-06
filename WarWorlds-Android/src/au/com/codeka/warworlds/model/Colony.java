package au.com.codeka.warworlds.model;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Colony {
    private static Logger log = LoggerFactory.getLogger(Colony.class);

    private String mKey;
    private String mPlanetKey;
    private String mStarKey;
    private long mPopulation;
    private String mEmpireKey;
    private double mFarmingFocus;
    private Date mLastSimulation;
    private double mFarmingRate;
    private double mPopulationRate;

    public String getKey() {
        return mKey;
    }
    public String getPlanetKey() {
        return mPlanetKey;
    }
    public String getStarKey() {
        return mStarKey;
    }
    public String getEmpireKey() {
        return mEmpireKey;
    }
    public long getPopulation() {
        return mPopulation;
    }
    public double getFarmingFocus() {
        return mFarmingFocus;
    }
    public double getFarmingRate() {
        return mFarmingRate;
    }
    public double getPopulationRate() {
        return mPopulationRate;
    }

    /**
     * Simulates this colony up to the current time. Simulation occurs in 15 minute blocks.
     */
    public void simulate(Planet planet) {
        Date now = new Date();
        long millis = now.getTime() - mLastSimulation.getTime();

        double hours = (millis / 1000) / 60.0 / 60.0;
        while (hours > 0.25) {
            simulateStep(planet, 0.25);
            hours -= 0.25;
        }
        if (hours > 0.0) {
            simulateStep(planet, hours);
        }
    }

    /**
     * Does a single "step" of the simulation.
     * @param dt The amount of time to simulate, in hours.
     */
    private void simulateStep(Planet planet, double dt) {

        log.debug(String.format("Updating farming rate: pop=%.2f focus=%.2f congeniality=%.2f",
                mPopulation / 1000.0, mFarmingFocus, planet.getFarmingCongeniality() / 50.0));
        mFarmingRate = (mPopulation / 1000.0) * mFarmingFocus * (planet.getFarmingCongeniality() / 50.0);

        mPopulationRate = ((double) (planet.getPopulationCongeniality() - mPopulation) / planet.getPopulationCongeniality());
        log.debug(String.format("Updating population rate: init=%.2f", mPopulationRate));
        mPopulationRate *= mFarmingRate * (planet.getPopulationCongeniality() / 500.0);

        mPopulation += mPopulation * mPopulationRate * dt;
        if (mPopulation < 0) {
            mPopulation = 0;
        }
    }

    public static Colony fromProtocolBuffer(Planet planet, warworlds.Warworlds.Colony pb) {
        Colony c = new Colony();
        c.mKey = pb.getKey();
        c.mPlanetKey = pb.getPlanetKey();
        c.mStarKey = pb.getStarKey();
        c.mPopulation = pb.getPopulation();
        c.mEmpireKey = pb.getEmpireKey();
        c.mFarmingRate = 1.0f;
        c.mLastSimulation = new Date(pb.getLastSimulation() * 1000);

        // TODO
        c.mFarmingFocus = 0.75;

        // make sure the rates and stuff are current
        if (planet != null) {
            c.simulate(planet);
        }

        return c;
    }

}
