package au.com.codeka.warworlds.model;

public class Colony {
    private String mKey;
    private String mPlanetKey;
    private String mStarKey;
    private long mPopulation;
    private float mPopulationRate;
    private String mEmpireKey;

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
        // TODO: calcuate based on population + (population_rate * time_since_last_simulate)
        return mPopulation;
    }

    public static Colony fromProtocolBuffer(warworlds.Warworlds.Colony pb) {
        Colony c = new Colony();
        c.mKey = pb.getKey();
        c.mPlanetKey = pb.getPlanetKey();
        c.mStarKey = pb.getStarKey();
        c.mPopulation = pb.getPopulation();
        c.mPopulationRate = pb.getPopulationRate();
        c.mEmpireKey = pb.getEmpireKey();

        return c;
    }

}
