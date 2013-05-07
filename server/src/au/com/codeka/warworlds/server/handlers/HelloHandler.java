package au.com.codeka.warworlds.server.handlers;

import java.sql.ResultSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.ctrl.StatisticsController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Star;

public class HelloHandler extends RequestHandler {
    private final Logger log = LoggerFactory.getLogger(HelloHandler.class);

    @Override
    protected void put() throws RequestException {
        Messages.HelloRequest hello_request_pb = getRequestBody(Messages.HelloRequest.class);
        new StatisticsController().registerLogin(getSession().getEmpireID(), hello_request_pb);

        Messages.HelloResponse.Builder hello_response_pb = Messages.HelloResponse.newBuilder();

        // TODO: this could be cached...
        String motd = "";
        try (SqlStmt stmt = DB.prepare("SELECT motd FROM motd")) {
            motd = stmt.selectFirstValue(String.class);
        } catch (Exception e) {
            throw new RequestException(e);
        }
        hello_response_pb.setMotd(Messages.MessageOfTheDay.newBuilder()
                                          .setMessage(motd)
                                          .setLastUpdate(""));

        // fetch the empire we're interested in
        Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
        if (empire != null) {
            // make sure they still have some colonies...
            int numColonies = 0;
            int[] stars = new EmpireController().getStarsForEmpire(getSession().getEmpireID());
            for (Star star : new StarController().getStars(stars)) {
                for (BaseColony baseColony : star.getColonies()) {
                    Colony colony = (Colony) baseColony;
                    if (colony.getEmpireID() == empire.getID()) {
                        numColonies ++;
                    }
                }
            }
            if (numColonies == 0) {
                log.info(String.format("Empire #%d [%s] has been wiped out, resetting.", empire.getID(), empire.getDisplayName()));
                new EmpireController().createEmpire(empire);
                hello_response_pb.setWasEmpireReset(true);
            }

            Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
            empire.toProtocolBuffer(empire_pb);
            hello_response_pb.setEmpire(empire_pb);

            // set up the initial building statistics
            String sql = "SELECT design_id, COUNT(*) FROM buildings WHERE empire_id = ? GROUP BY design_id";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, empire.getID());
                ResultSet rs = stmt.select();

                Messages.EmpireBuildingStatistics.Builder build_stats_pb = Messages.EmpireBuildingStatistics.newBuilder();
                while (rs.next()) {
                    String designID = rs.getString(1);
                    int num = rs.getInt(2);

                    Messages.EmpireBuildingStatistics.DesignCount.Builder design_count_pb = Messages.EmpireBuildingStatistics.DesignCount.newBuilder();
                    design_count_pb.setDesignId(designID);
                    design_count_pb.setNumBuildings(num);
                    build_stats_pb.addCounts(design_count_pb);
                }
                hello_response_pb.setBuildingStatistics(build_stats_pb);
            } catch (Exception e) {
                throw new RequestException(e);
            }
        }

        setResponseBody(hello_response_pb.build());
    }
}
