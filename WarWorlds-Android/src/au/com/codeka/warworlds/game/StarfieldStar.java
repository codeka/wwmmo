package au.com.codeka.warworlds.game;

import au.com.codeka.warworlds.common.util.CoolRandom;

public class StarfieldStar {

    //private StarfieldNode mNode;
    private StarfieldSector mSector;
    private int mOffsetX;
    private int mOffsetY;
    private int mColour;
    private int mSize;

    public StarfieldStar(StarfieldNode node, StarfieldSector sector) {
        //mNode = node;
        mSector = sector;

        CoolRandom r = mSector.getRandom();
        mOffsetX = r.nextInt(-12, 12);
        mOffsetY = r.nextInt(-12, 12);

        int red = r.nextInt(100, 255);
        int green = r.nextInt(100, 255);
        int blue = r.nextInt(100, 255);
        mColour = 0xFF000000 | (red << 16) | (green << 8) | (blue);

        mSize = r.nextInt(8, 12);
    }

    public int getOffsetX() {
        return mOffsetX;
    }

    public int getOffsetY() {
        return mOffsetY;
    }

    public int getColour() {
        return mColour;
    }

    public int getSize() {
        return mSize;
    }
}
