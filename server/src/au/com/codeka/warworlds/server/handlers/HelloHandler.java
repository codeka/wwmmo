package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.SessionController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.StatisticsController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Star;

public class HelloHandler extends RequestHandler {
    private final Logger log = LoggerFactory.getLogger(HelloHandler.class);

    @Override
    protected void get() throws RequestException {
        // this is just used for testing, nothing more...
        if (!getSession().isAdmin()) {
            throw new RequestException(501);
        }

        Messages.HelloRequest hello_request_pb = Messages.HelloRequest.newBuilder()
                .setAllowInlineNotfications(false)
                .setDeviceBuild("TEST_DEVICE_BUILD")
                .setDeviceManufacturer("TEST_DEVICE_MANUFACTURER")
                .setDeviceModel("TEST_DEVICE_MODEL")
                .setDeviceVersion("TEST_DEVICE_VERSION")
                .setMemoryClass(0)
                .build();
        processHello(hello_request_pb);
    }

    @Override
    protected void put() throws RequestException {
        Messages.HelloRequest hello_request_pb = getRequestBody(Messages.HelloRequest.class);

        processHello(hello_request_pb);
    }

    private void processHello(Messages.HelloRequest hello_request_pb) throws RequestException {
        Session sess = getSession();
        if (hello_request_pb.hasAllowInlineNotfications()) {
            sess.setAllowInlineNotifications(hello_request_pb.getAllowInlineNotfications());
            new SessionController().saveSession(sess);
        }

        Messages.HelloResponse.Builder hello_response_pb = Messages.HelloResponse.newBuilder();

        // damn, this is why things should never be marked "required" in protobufs!
        hello_response_pb.setMotd(Messages.MessageOfTheDay.newBuilder().setMessage("").setLastUpdate(""));

        // fetch the empire we're interested in
        Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
        if (empire != null) {
            new StatisticsController().registerLogin(getSession().getEmpireID(), hello_request_pb);
            if (empire.getState() == Empire.State.ABANDONED) {
                new EmpireController().markActive(empire);
            }

            // TODO: remove this
            ArrayList<Integer> starIDs = new EmpireController().getStarsForEmpire(
                    getSession().getEmpireID(), EmpireController.EmpireStarsFilter.Everything, null);
            if (!findColony(starIDs, getSession().getEmpireID())) {
                log.info(String.format("Empire #%d [%s] has been wiped out, resetting.", empire.getID(), empire.getDisplayName()));
                new EmpireController().createEmpire(empire);
                hello_response_pb.setWasEmpireReset(true);

                String resetReason = new EmpireController().getResetReason(empire.getID());
                if (resetReason != null) {
                    hello_response_pb.setEmpireResetReason(resetReason);
                }
            }

            Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
            empire.toProtocolBuffer(empire_pb, true);
            hello_response_pb.setEmpire(empire_pb);

            // set up the initial building statistics
            String sql = "SELECT design_id, COUNT(*) FROM buildings WHERE empire_id = ? GROUP BY design_id";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, empire.getID());
                SqlResult res = stmt.select();

                Messages.EmpireBuildingStatistics.Builder build_stats_pb = Messages.EmpireBuildingStatistics.newBuilder();
                while (res.next()) {
                    String designID = res.getString(1);
                    int num = res.getInt(2);

                    Messages.EmpireBuildingStatistics.DesignCount.Builder design_count_pb = Messages.EmpireBuildingStatistics.DesignCount.newBuilder();
                    design_count_pb.setDesignId(designID);
                    design_count_pb.setNumBuildings(num);
                    build_stats_pb.addCounts(design_count_pb);
                }
                hello_response_pb.setBuildingStatistics(build_stats_pb);
            } catch (Exception e) {
                throw new RequestException(e);
            }

            // if we're set to force ignore ads, make sure we pass that along
            hello_response_pb.setForceRemoveAds(empire.getForceRemoveAds());

            if (!hello_request_pb.hasNoStarList() || !hello_request_pb.getNoStarList()) {
                // grab all of the empire's stars (except markers and wormholes) and send across the identifiers
                sql = "SELECT id, name" +
                     " FROM stars" +
                     " INNER JOIN (SELECT DISTINCT star_id FROM colonies WHERE empire_id = ?" +
                                 " UNION SELECT DISTINCT star_id FROM fleets WHERE empire_id = ?) as s" +
                       " ON s.star_id = stars.id" +
                     " WHERE star_type NOT IN (" + Star.Type.Marker.ordinal() + ", " + Star.Type.Wormhole.ordinal() + ")" +
                     " ORDER BY name ASC";
                try (SqlStmt stmt = DB.prepare(sql)) {
                    stmt.setInt(1, empire.getID());
                    stmt.setInt(2, empire.getID());
                    SqlResult res = stmt.select();
    
                    while (res.next()) {
                        hello_response_pb.addStarIds(res.getLong(1));
                    }
                } catch (Exception e) {
                    throw new RequestException(e);
                }
            }
        }

        setResponseBody(hello_response_pb.build());
    }

    /** Goes through the given stars and looks for at least one colony. Returns true if one is
        found. */
    private boolean findColony(ArrayList<Integer> starIDs, int empireID) throws RequestException {
        // do it in groups of 10 stars, since most of the time we'll find it in the first group
        // and there's no point querying all, especially if they have a lot of stars.
        for (int startIndex = 0; startIndex < starIDs.size(); startIndex++) {
            int endIndex = startIndex + 10;
            if (endIndex >= starIDs.size()) {
                endIndex = starIDs.size() - 1;
            }

            List<Integer> sublist = starIDs.subList(startIndex, endIndex);
            List<Star> stars = new StarController().getStars(sublist);
            if (findColony(stars, empireID)) {
                return true;
            }
        }

        return false;
    }

    private boolean findColony(List<Star> stars, int empireID) {
        for (Star star : stars) {
            for (BaseColony baseColony : star.getColonies()) {
                Colony colony = (Colony) baseColony;
                if (colony.getEmpireID() != null && empireID == colony.getEmpireID()) {
                    return true;
                }
            }
        }
        return false;
    }
}
