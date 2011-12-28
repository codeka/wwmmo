package au.com.codeka.warworlds.shared;

import java.io.Serializable;

/**
 * A \c StarfieldSector represents a "sector" of the starfield. It is 
 * representend by a virtual "field", 256x256 pixels big of somewhere between
 * ~10 & 15 \c StarfieldStar objects. 
 */
public class StarfieldSector implements Serializable {
    private static final long serialVersionUID = 1L;

    private StarfieldStar[] mStars;
    private long mSectorX;
    private long mSectorY;

    public StarfieldSector() {
    }

    public StarfieldSector(long sectorX, long sectorY, StarfieldStar[] stars) {
        mSectorX = sectorX;
        mSectorY = sectorY;
        mStars = stars;
    }

    public StarfieldStar[] getStars() {
        return mStars;
    }

    public long getSectorX() {
        return mSectorX;
    }

    public long getSectorY() {
        return mSectorY;
    }
}
