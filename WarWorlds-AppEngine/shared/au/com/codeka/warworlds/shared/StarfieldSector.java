package au.com.codeka.warworlds.shared;

import java.io.Serializable;

/**
 * A \c StarfieldSector represents a "sector" of the starfield. It contains a
 * grid of 16x16 \c StarfieldNode, which each contains zero or one stars. 
 * @author dean@codeka.com.au
 *
 */
public class StarfieldSector implements Serializable {
    private static final long serialVersionUID = 1L;

    private StarfieldNode[] mNodes;
    private long mSectorX;
    private long mSectorY;

    public StarfieldSector() {
    }

    public StarfieldSector(long sectorX, long sectorY, StarfieldNode[] nodes) {
        mSectorX = sectorX;
        mSectorY = sectorY;
        mNodes = nodes;
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

    public StarfieldNode[] getNodes() {
        return mNodes;
    }

    public long getSectorX() {
        return mSectorX;
    }

    public long getSectorY() {
        return mSectorY;
    }
}
