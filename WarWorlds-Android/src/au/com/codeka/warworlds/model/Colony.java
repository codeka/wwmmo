package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Colony {
    private String mKey;
    private String mPlanetKey;
    private String mStarKey;
    private long mPopulation;
    private String mEmpireKey;
    private float mFarmingFocus;
    private float mConstructionFocus;
    private float mPopulationFocus;
    private float mMiningFocus;
    private Date mLastSimulation;
    private List<Building> mBuildings;

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
    public float getFarmingFocus() {
        return mFarmingFocus;
    }
    public float getConstructionFocus() {
        return mConstructionFocus;
    }
    public float getPopulationFocus() {
        return mPopulationFocus;
    }
    public float getMiningFocus() {
        return mMiningFocus;
    }
    public Date getLastSimulation() {
        return mLastSimulation;
    }
    public List<Building> getBuildings() {
        return mBuildings;
    }

    public static Colony fromProtocolBuffer(warworlds.Warworlds.Colony pb) {
        Colony c = new Colony();
        c.mKey = pb.getKey();
        c.mPlanetKey = pb.getPlanetKey();
        c.mStarKey = pb.getStarKey();
        c.mPopulation = pb.getPopulation();
        c.mEmpireKey = pb.getEmpireKey();
        c.mLastSimulation = new Date(pb.getLastSimulation() * 1000);
        c.mBuildings = new ArrayList<Building>();
        c.mFarmingFocus = pb.getFocusFarming();
        c.mConstructionFocus = pb.getFocusConstruction();
        c.mMiningFocus = pb.getFocusMining();
        c.mPopulationFocus = pb.getFocusPopulation();

        return c;
    }

}
