package au.com.codeka.warworlds.model;


public class Star {
    private Sector mSector;
    private String mKey;
    private String mName;
    private int mColour;
    private int mSize;
    private int mOffsetX;
    private int mOffsetY;
    private int mNumPlanets;
    private Planet[] mPlanets;

    public Star() {
        mSector = null; // can be null if we're fetched separately from the sector
        mPlanets = null; // can be null if planets have not been populated...
    }

    public Sector getSector() {
        return mSector;
    }
    public String getKey() {
        return mKey;
    }
    public String getName() {
        return mName;
    }
    public int getColour() {
        return mColour;
    }
    public int getSize() {
        return mSize;
    }
    public int getOffsetX() {
        return mOffsetX;
    }
    public int getOffsetY() {
        return mOffsetY;
    }
    public int getNumPlanets() {
        if (mPlanets == null) {
            return mNumPlanets;
        } else {
            return mPlanets.length;
        }
    }
    public Planet[] getPlanets() {
        return mPlanets;
    }

    public void setDummySector(long sectorX, long sectorY) {
        mSector = new Sector.DummySector(sectorX, sectorY);
    }

    public static Star fromProtocolBuffer(warworlds.Warworlds.Star pb) {
        return fromProtocolBuffer(null, pb);
    }

    /**
     * Converts the given Star protocol buffer into a \c Star.
     */
    public static Star fromProtocolBuffer(Sector sector, warworlds.Warworlds.Star pb) {
        Star s = new Star();
        s.mSector = sector;
        s.mKey = pb.getKey();
        s.mName = pb.getName();
        s.mColour = pb.getColour();
        s.mSize = pb.getSize();
        s.mOffsetX = pb.getOffsetX();
        s.mOffsetY = pb.getOffsetY();
        s.mNumPlanets = pb.getNumPlanets();

        int numPlanets = pb.getPlanetsCount();
        if (numPlanets > 0) {
            s.mPlanets = new Planet[numPlanets];
            for (int i = 0; i < numPlanets; i++) {
                s.mPlanets[i] = Planet.fromProtocolBuffer(s, pb.getPlanets(i));
            }
        }

        return s;
    }
}
