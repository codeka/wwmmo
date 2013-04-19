package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

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

    public void fromProtocolBuffer(Messages.Sector pb) {
        mX = pb.getX();
        mY = pb.getY();
        for (Messages.Star star_pb : pb.getStarsList()) {
            mStars.add(createStar(star_pb));
        }

        // could this be more efficient? there's not a lot of stars, so maybe not a big deal
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
