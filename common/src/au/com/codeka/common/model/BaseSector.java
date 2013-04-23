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
        float dx = a.getOffsetX() - b.getOffsetX();
        float dy = a.getOffsetY() - b.getOffsetY();

        float dsx = a.getSectorX() - b.getSectorX();
        dx += (dsx * SECTOR_SIZE);

        float dsy = a.getSectorY() - b.getSectorY();
        dy += (dsy * SECTOR_SIZE);

        return new Vector2(dx, dy);
    }

    /**
     * Calculates the distance (in "parsecs") between the two given stars.
     */
    public static float distanceInParsecs(BaseStar a, BaseStar b) {
        double distanceInPixels = directionBetween(a, b).length();
        return (float) (distanceInPixels / 10.0);
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
