package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Vector2;
import au.com.codeka.common.protobuf.Messages;

/**
 * A \c Sector represents a "section" of space, with corresponding stars, planets and so on.
 */
public abstract class BaseSector {
    protected long mX;
    protected long mY;
    protected List<BaseStar> mStars = new ArrayList<BaseStar>();

    protected abstract BaseStar createStar(Messages.Star pb);
    protected abstract BaseColony createColony(Messages.Colony pb);
    protected abstract BaseFleet createFleet(Messages.Fleet pb);

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

    public void fromProtocolBuffer(Messages.Sector pb) {
        mX = pb.getX();
        mY = pb.getY();
        for (Messages.Star star_pb : pb.getStarsList()) {
            mStars.add(createStar(star_pb));
        }

        // could this be more efficient? there's not a lot of stars, so maybe not a big deal
        // TODO: these fields will be removed when the ALPHA server is closed...
        for (Messages.Colony colony_pb : pb.getColoniesList()) {
            if (colony_pb.getPopulation() < 1.0) {
                // colonies with zero population are dead -- they just don't know it yet.
                continue;
            }

            for (BaseStar star : mStars) {
                if (colony_pb.getStarKey().equals(star.getKey())) {
                    star.addColony(createColony(colony_pb));
                    break;
                }
            }
        }

        for (Messages.Fleet fleet_pb : pb.getFleetsList()) {
            for (BaseStar star : mStars) {
                if (fleet_pb.getStarKey().equals(star.getKey())) {
                    star.addFleet(createFleet(fleet_pb));
                    break;
                }
            }
        }
    }

    public void toProtocolBuffer(Messages.Sector.Builder pb) {
        pb.setX(mX);
        pb.setY(mY);

        for (BaseStar star : mStars) {
            Messages.Star.Builder star_pb = Messages.Star.newBuilder();
            star.toProtocolBuffer(star_pb, true);
            pb.addStars(star_pb);
        }

        // TODO colonies
        // TODO fleets
    }
}
