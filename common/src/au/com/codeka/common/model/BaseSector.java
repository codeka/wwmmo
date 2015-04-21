package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Vector2;
import au.com.codeka.common.protobuf.Colony;
import au.com.codeka.common.protobuf.Fleet;
import au.com.codeka.common.protobuf.Sector;
import au.com.codeka.common.protobuf.Star;

/**
 * A \c Sector represents a "section" of space, with corresponding stars, planets and so on.
 */
public abstract class BaseSector {
    protected long mX;
    protected long mY;
    protected List<BaseStar> mStars = new ArrayList<BaseStar>();

    protected abstract BaseStar createStar(Star pb);

    public static int SECTOR_SIZE = 1024;
    public static float PIXELS_PER_PARSEC = 10.0f;

    public long getX() {
        return mX;
    }
    public long getY() {
        return mY;
    }
    public List<BaseStar> getStars() {
        return mStars;
    }

    /**
     * Returns a {@link Vector2} that represents a line segment from {@link Star} a to
     * {@link Star} b. You can use the \c length() method to determine the distance between them.
     */
    public static Vector2 directionBetween(BaseStar a, BaseStar b) {
        return directionBetween(a.getSectorX(), a.getSectorY(), a.getOffsetX(), a.getOffsetY(),
                                b.getSectorX(), b.getSectorY(), b.getOffsetX(), b.getOffsetY());
    }

    public static Vector2 directionBetween(long sector1X, long sector1Y, int offset1X, int offset1Y,
                                           long sector2X, long sector2Y, int offset2X, int offset2Y) {
        float dx = offset2X - offset1X;
        float dy = offset2Y - offset1Y;

        float dsx = sector2X - sector1X;
        dx += (dsx * SECTOR_SIZE);

        float dsy = sector2Y - sector1Y;
        dy += (dsy * SECTOR_SIZE);

        return new Vector2(dx / PIXELS_PER_PARSEC, dy / PIXELS_PER_PARSEC);
    }

    /**
     * Calculates the distance (in "parsecs") between the two given stars.
     */
    public static float distanceInParsecs(BaseStar a, BaseStar b) {
        return (float) directionBetween(a, b).length();
    }

    public static float distanceInParsecs(BaseStar a, long sectorX, long sectorY, int offsetX, int offsetY) {
        return (float) directionBetween(a.getSectorX(), a.getSectorY(), a.getOffsetX(), a.getOffsetY(),
                                        sectorX, sectorY, offsetX, offsetY).length();
    }

    public static float distanceInParsecs(long sector1X, long sector1Y, int offset1X, int offset1Y,
                                          long sector2X, long sector2Y, int offset2X, int offset2Y) {
        return (float) directionBetween(sector1X, sector1Y, offset1X, offset1Y,
                                        sector2X, sector2Y, offset2X, offset2Y).length();
    }

    public void fromProtocolBuffer(Sector pb) {
        mX = pb.x;
        mY = pb.y;
        for (Star star_pb : pb.stars) {
            mStars.add(createStar(star_pb));
        }
    }

    public void toProtocolBuffer(Sector.Builder pb) {
        pb.x = mX;
        pb.y = mY;

        pb.stars = new ArrayList<>();
        for (BaseStar star : mStars) {
            Star.Builder star_pb = new Star.Builder();
            star.toProtocolBuffer(star_pb, true);
            pb.stars.add(star_pb.build());
        }
    }
}
