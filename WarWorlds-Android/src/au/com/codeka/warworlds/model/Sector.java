package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A \c Sector represents a "section" of space, with corresponding stars, planets and so on.
 */
public class Sector {

    protected long mX;
    protected long mY;
    private List<Star> mStars;

    public Sector() {
        mStars = new ArrayList<Star>();
    }

    public long getX() {
        return mX;
    }
    public long getY() {
        return mY;
    }
    public List<Star> getStars() {
        return mStars;
    }

    /**
     * Converts a list of Sector protocol buffers to a list of \c Sector
     * @param pb
     * @return
     */
    public static List<Sector> fromProtocolBuffer(List<warworlds.Warworlds.Sector> pb) {
        ArrayList<Sector> sectors = new ArrayList<Sector>();
        for(warworlds.Warworlds.Sector sector_pb : pb) {
            sectors.add(fromProtocolBuffer(sector_pb));
        }
        return sectors;
    }

    /**
     * Converts a single protocol buffer \c Sector into a \c Sector class.
     */
    public static Sector fromProtocolBuffer(warworlds.Warworlds.Sector pb) {
        Sector s = new Sector();
        s.mX = pb.getX();
        s.mY = pb.getY();
        for (warworlds.Warworlds.Star star_pb : pb.getStarsList()) {
            s.mStars.add(Star.fromProtocolBuffer(s, star_pb));
        }

        return s;
    }

    /**
     * Sometimes a star will just have a reference to a "dummy" sector (this class) so that
     * we can keep a reference to the sector's (x,y) coordinates.
     */
    public static class DummySector extends Sector {
        public DummySector(long x, long y) {
            mX = x;
            mY = y;
        }
    }
}
