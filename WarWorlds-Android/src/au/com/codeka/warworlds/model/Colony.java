package au.com.codeka.warworlds.model;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

public class Colony {
    private String mKey;
    private String mPlanetKey;
    private String mStarKey;
    private float mPopulation;
    private String mEmpireKey;
    private float mFarmingFocus;
    private float mConstructionFocus;
    private float mPopulationFocus;
    private float mMiningFocus;
    private float mPopulationDelta;
    private float mGoodsDelta;
    private float mMineralsDelta;
    private DateTime mLastSimulation;
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
    public float getPopulation() {
        return mPopulation;
    }
    public float getFarmingFocus() {
        return mFarmingFocus;
    }
    public void setFarmingFocus(float focus) {
        mFarmingFocus = focus;
    }
    public float getConstructionFocus() {
        return mConstructionFocus;
    }
    public void setConstructionFocus(float focus) {
        mConstructionFocus = focus;
    }
    public float getPopulationFocus() {
        return mPopulationFocus;
    }
    public void setPopulationFocus(float focus) {
        mPopulationFocus = focus;
    }
    public float getMiningFocus() {
        return mMiningFocus;
    }
    public void setMiningFocus(float focus) {
        mMiningFocus = focus;
    }
    public float getPopulationDelta() {
        return mPopulationDelta;
    }
    public float getGoodsDelta() {
        return mGoodsDelta;
    }
    public float getMineralsDelta() {
        return mMineralsDelta;
    }
    public DateTime getLastSimulation() {
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
        c.mLastSimulation = new DateTime(pb.getLastSimulation() * 1000, DateTimeZone.UTC);
        c.mBuildings = new ArrayList<Building>();
        c.mFarmingFocus = pb.getFocusFarming();
        c.mConstructionFocus = pb.getFocusConstruction();
        c.mMiningFocus = pb.getFocusMining();
        c.mPopulationFocus = pb.getFocusPopulation();
        c.mPopulationDelta = pb.getDeltaPopulation();
        c.mGoodsDelta = pb.getDeltaGoods();
        c.mMineralsDelta = pb.getDeltaMinerals();

        return c;
    }

    public warworlds.Warworlds.Colony toProtocolBuffer() {
        return warworlds.Warworlds.Colony.newBuilder()
            .setKey(getKey())
            .setPlanetKey(getPlanetKey())
            .setStarKey(getStarKey())
            .setEmpireKey(getEmpireKey())
            .setPopulation(getPopulation())
            .setFocusPopulation(getPopulationFocus())
            .setFocusFarming(getFarmingFocus())
            .setFocusMining(getMiningFocus())
            .setFocusConstruction(getConstructionFocus())
            .build();
    }
}
