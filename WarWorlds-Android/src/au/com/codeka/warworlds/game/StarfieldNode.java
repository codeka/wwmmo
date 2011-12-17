package au.com.codeka.warworlds.game;

import au.com.codeka.warworlds.common.util.CoolRandom;

public class StarfieldNode {
    private StarfieldSector mSector;
    private StarfieldStar mStar;
    private int mNodeX;
    private int mNodeY;

    public StarfieldNode(int nodeX, int nodeY, StarfieldSector sector) {
        mNodeX = nodeX;
        mNodeY = nodeY;
        mSector = sector;

        CoolRandom r = mSector.getRandom();
        if (r.nextBoolean(0.06f)) {
            mStar = new StarfieldStar(this, mSector);
        }
    }

    public int getNodeX() {
        return mNodeX;
    }

    public int getNodeY() {
        return mNodeY;
    }

    public StarfieldStar getStar() {
        return mStar;
    }
}
