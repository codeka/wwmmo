package au.com.codeka.warworlds.game;

import au.com.codeka.warworlds.common.util.CoolRandom;

/**
 * A \c StarfieldSector represents a "sector" of the starfield. It contains a
 * grid of 16x16 \c StarfieldNode, which each contains zero or one stars. 
 * @author dean@codeka.com.au
 *
 */
public class StarfieldSector {
    private StarfieldNode[] mNodes;
    private long mSectorX;
    private long mSectorY;
    private CoolRandom mRandom;

    public StarfieldSector(long sectorX, long sectorY) {
        mSectorX = sectorX;
        mSectorY = sectorY;
        mNodes = new StarfieldNode[256];
        mRandom = new CoolRandom(sectorX * 347, sectorY * -374);

        for(int y = 0; y < 16; y++) {
            for(int x = 0; x < 16; x++) {
                mNodes[y*16 + x] = new StarfieldNode(x, y, this);
            }
        }
    }

    public StarfieldNode getNode(int nodeX, int nodeY) {
        if (nodeX >= 16 || nodeX < 0) {
            throw new IllegalArgumentException("Invalid nodeX "+nodeX);
        }
        if (nodeY >= 16 || nodeY < 0) {
            throw new IllegalArgumentException("Invalid nodeY "+nodeY);
        }

        return mNodes[nodeY*16+nodeX];
    }

    public long getSectorX() {
        return mSectorX;
    }

    public long getSectorY() {
        return mSectorY;
    }
    
    public CoolRandom getRandom() {
        return mRandom;
    }
}
