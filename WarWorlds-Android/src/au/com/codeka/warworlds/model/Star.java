package au.com.codeka.warworlds.model;

import java.util.List;

public class Star {

    private String mName;
    private int mColour;
    private int mSize;
    private int mOffsetX;
    private int mOffsetY;
    private int mNumPlanets;
    private List<Planet> mPlanets;

    public Star() {
        mPlanets = null; // can be null if planets have not been populated...
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
            return mPlanets.size();
        }
    }
    public List<Planet> getPlanets() {
        return mPlanets;
    }

    /**
     * Converts the given Star protocol buffer into a \c Star.
     */
    public static Star fromProtocolBuffer(warworlds.Warworlds.Star pb) {
        Star s = new Star();
        s.mName = pb.getName();
        s.mColour = pb.getColour();
        s.mSize = pb.getSize();
        s.mOffsetX = pb.getOffsetX();
        s.mOffsetY = pb.getOffsetY();
        s.mNumPlanets = pb.getNumPlanets();
        // TODO: planets..
        
        return s;
    }
}
