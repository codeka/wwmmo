package au.com.codeka.common.model;

import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Seconds;

import au.com.codeka.common.Cash;
import au.com.codeka.common.Vector2;

import com.squareup.wire.Wire;

public class Model {
    public static Wire wire = new Wire();

    public static int SECTOR_SIZE = 1024;
    public static int PIXELS_PER_PARSEC = 10;

    public static DateTime toDateTime(Long epoch) {
        if (epoch == null) {
            return null;
        }
        return new DateTime(epoch * 1000, DateTimeZone.UTC);
    }

    public static Long fromDateTime(DateTime dt) {
        if (dt == null) {
            return null;
        }
        return dt.getMillis() / 1000;
    }

    public static Fleet findFleet(Star star, String fleetKey) {
        for (Fleet fleet : star.fleets) {
            if (fleet.key.equals(fleetKey)) {
                return fleet;
            }
        }
        return null;
    }

    public static void move(Fleet fleet, DateTime now, String destinationStarKey, DateTime eta) {
        fleet.state = Fleet.FLEET_STATE.MOVING;
        fleet.state_start_time = fromDateTime(now);
        fleet.destination_star_key = destinationStarKey;
        fleet.eta = fromDateTime(eta);
        fleet.target_fleet_key = null;
    }

    public static void idle(Fleet fleet, DateTime now) {
        fleet.state = Fleet.FLEET_STATE.IDLE;
        fleet.state_start_time = fromDateTime(now);
        fleet.destination_star_key = null;
        fleet.eta = null;
        fleet.target_fleet_key = null;
    }

    public static void attack(Fleet fleet, DateTime now) {
        fleet.state = Fleet.FLEET_STATE.ATTACKING;
        fleet.state_start_time = fromDateTime(now);
        fleet.destination_star_key = null;
        fleet.eta = null;
        fleet.target_fleet_key = null;
    }

    public static float getTimeToDestination(Fleet fleet, Star srcStar, Star destStar) {
        if (fleet.eta == null) {
            return 0.0f;
        }

        DateTime now = DateTime.now(DateTimeZone.UTC);
        return (Seconds.secondsBetween(now, toDateTime(fleet.eta)).getSeconds() / 3600.0f);
    }


    /**
     * Returns a {@link Vector2} that represents a line segment from {@link Star} a to
     * {@link Star} b. You can use the \c length() method to determine the distance between them.
     */
    public static Vector2 directionBetween(Star a, Star b) {
        return directionBetween(a.sector_x, a.sector_y, a.offset_x, a.offset_y,
                                b.sector_x, b.sector_y, b.offset_x, b.offset_y);
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
    public static float distanceInParsecs(Star a, Star b) {
        return (float) directionBetween(a, b).length();
    }

    public static float distanceInParsecs(Star a, long sectorX, long sectorY, int offsetX, int offsetY) {
        return (float) directionBetween(a.sector_x, a.sector_y, a.offset_x, a.offset_y,
                                        sectorX, sectorY, offsetX, offsetY).length();
    }

    public static float distanceInParsecs(long sector1X, long sector1Y, int offset1X, int offset1Y,
                                          long sector2X, long sector2Y, int offset2X, int offset2Y) {
        return (float) directionBetween(sector1X, sector1Y, offset1X, offset1Y,
                                        sector2X, sector2Y, offset2X, offset2Y).length();
    }

    public static boolean isInCooldown(Colony colony) {
        long now = System.currentTimeMillis();
        return (now < colony.cooldown_end_time);
    }

    public static String getDescription(AllianceRequest request) {
        switch (request.request_type) {
        case JOIN:
            return "Join";
        case LEAVE:
            return "Leave";
        case KICK:
            return "KICK";
        case DEPOSIT_CASH:
            return String.format("Despoit %s", Cash.format(request.amount));
        case WITHDRAW_CASH:
            return String.format("Withdraw: %s", Cash.format(request.amount));
        default:
            return "Unknown!";
        }
    }

    /** Gets the progress of the given @see BuildRequest, interpolated based on the remaining time. */
    public static float getProgress(BuildRequest br) {
        // mProgress will be the accurate at the time this BuildRequest was refreshed from the
        // server. We'll do a little bit of interpolation so that it's a good estimate *after*
        // we've been refreshed from the server, too.
        DateTime endTime = toDateTime(br.end_time);
        DateTime now = DateTime.now(DateTimeZone.UTC);
        if (endTime.isBefore(now)) {
            return 1.0f;
        }

        long numerator = now.getMillis() - br.getConstructTime();
        long denominator = br.end_time - br.getConstructTime();
        float percentRemaining = (float) numerator / (float) denominator;

        return br.progress + ((1.0f - br.progress) * percentRemaining);
    }

    /** Gets the progress, 0..100 of the given @see BuildRequest */
    public static float getPercentComplete(BuildRequest br) {
        float percent = getProgress(br) * 100.0f;
        if (percent < 0)
            percent = 0;
        if (percent > 100)
            percent = 100;
        return percent;
    }

    /** Calculates a {@see Duration} object that describes approximately how long the given build has left. */
    public static Duration getRemainingTime(BuildRequest br) {
        long now = System.currentTimeMillis();
        if (br.end_time < now) {
            return Duration.ZERO;
        }

        Duration d = Duration.millis(br.end_time - now);
        // if it's actually zero, add a few seconds (to differentiate between "REALLY now" and
        // "it'll never finish, so lets return zero")
        if (d.compareTo(Duration.standardSeconds(5)) <  0)
            return Duration.standardSeconds(5);
        return d;
    }

    public static int getTotalPossibleVotes(Alliance alliance, Set<Integer> excludingEmpires) {
        int totalVotes = 0;
        for (AllianceMember member : alliance.members) {
            int memberEmpireID = Integer.parseInt(member.empire_key);
            if (excludingEmpires.contains(memberEmpireID)) {
                continue;
            }

            totalVotes += getNumVotesForRank(member.rank);
        }
        return totalVotes;
    }

    public static int getNumVotesForRank(AllianceMember.Rank rank) {
        switch(rank) {
        case CAPTAIN:
            return 10;
        case LIEUTENANT:
            return 5;
        case MEMBER:
            return 1;
        }

        return 0;
    }

    public static int getRequiredVotes(AllianceRequest.RequestType requestType) {
        switch (requestType) {
        case JOIN:
            return 5;
        case LEAVE:
            return 0;
        case KICK:
            return 10;
        case DEPOSIT_CASH: 
            return 0;
        case WITHDRAW_CASH:
            return 10;
        }
        return 0;
    }
}
