package au.com.codeka.warworlds.model;


public class Planet {
    private static PlanetType[] sPlanetTypes = {
        new PlanetType("Gas Giant"),
        new PlanetType("Radiated"),
        new PlanetType("Inferno"),
        new PlanetType("Asteroids"),
        new PlanetType("Water"),
        new PlanetType("Toxic"),
        new PlanetType("Desert"),
        new PlanetType("Swamp"),
        new PlanetType("Terran")
    };

    private Star mStar;
    private int mIndex;
    private PlanetType mPlanetType;
    private int mSize;

    public Star getStar() {
        return mStar;
    }

    public int getIndex() {
        return mIndex;
    }

    public PlanetType getPlanetType() {
        return mPlanetType;
    }

    public int getSize() {
        return mSize;
    }

    /**
     * Converts the given Planet protocol buffer into a \c Planet.
     */
    public static Planet fromProtocolBuffer(Star star, warworlds.Warworlds.Planet pb) {
        Planet p = new Planet();
        p.mStar = star;
        p.mIndex = pb.getIndex();
        p.mPlanetType = sPlanetTypes[pb.getPlanetType().getNumber() - 1];
        p.mSize = pb.getSize();

        return p;
    }

    public static class PlanetType {
        private String mDisplayName;

        public PlanetType(String displayName) {
            mDisplayName = displayName;
        }

        public String getDisplayName() {
            return mDisplayName;
        }
    }
}
