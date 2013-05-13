package au.com.codeka.warworlds.server.events;

import java.sql.ResultSet;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.DesignKind;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Event;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.BuildingController;
import au.com.codeka.warworlds.server.ctrl.ColonyController;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.FleetController;
import au.com.codeka.warworlds.server.ctrl.SituationReportController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class BuildCompleteEvent extends Event {
    private final Logger log = LoggerFactory.getLogger(BuildCompleteEvent.class);

    @Override
    public String getNextEventTimeSql() {
        return "SELECT MIN(end_time) FROM build_requests";
    }

    @Override
    public void process() {
        ArrayList<Integer> processedIDs = new ArrayList<Integer>();
        String sql = "SELECT id, star_id, colony_id, empire_id, existing_building_id," +
                           " design_kind, design_id, count" +
                    " FROM build_requests" +
                    " WHERE end_time < ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, DateTime.now().plusSeconds(10)); // anything in the next 10 seconds is a candidate
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int id = rs.getInt(1);
                int starID = rs.getInt(2);
                int colonyID = rs.getInt(3);
                int empireID = rs.getInt(4);
                Integer existingBuildingID = rs.getInt(5);
                if (rs.wasNull()) {
                    existingBuildingID = null;
                }
                DesignKind designKind = DesignKind.fromNumber(rs.getInt(6));
                String designID = rs.getString(7);
                float count = rs.getFloat(8);

                Star star = new StarController().getStar(starID);
                Colony colony = null;
                for (BaseColony bc : star.getColonies()) {
                    if (((Colony) bc).getID() == colonyID) {
                        colony = (Colony) bc;
                    }
                }

                try {
                    processBuildRequest(id, star, colony, empireID, existingBuildingID, designKind, designID, count);
                } catch (Exception e) {
                    log.error("Error processing build-complete event!", e);
                }
                processedIDs.add(id);
            }
        } catch(Exception e) {
            log.error("Error processing build-complete event!", e);
            // TODO: errors?
        }

        if (processedIDs.isEmpty()) {
            return;
        }

        sql = "DELETE FROM build_requests WHERE id IN ";
        for (int i = 0; i < processedIDs.size(); i++) {
            if (i == 0) {
                sql += "(";
            } else {
                sql += ", ";
            }
            sql += processedIDs.get(i);
        }
        sql += ")";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.update();
        } catch(Exception e) {
            log.error("Error processing build-complete event!", e);
            // TODO: errors?
        }
    }

    private void processBuildRequest(int buildRequestID, Star star, Colony colony, int empireID,
                                     Integer existingBuildingID, DesignKind designKind,
                                     String designID, float count) throws RequestException {
        Simulation sim = new Simulation();
        sim.simulate(star);

        Fleet fleet = null;
        if (designKind == DesignKind.BUILDING) {
            processBuildingBuild(star, colony, empireID, existingBuildingID, designID);
        } else {
            fleet = processFleetBuild(star, colony, empireID, designID, count);
        }

        sim.simulate(star); // simulate again to re-calculate the end times
        new StarController().update(star);

        Messages.SituationReport.Builder sitrep_pb = Messages.SituationReport.newBuilder();
        sitrep_pb.setEmpireKey(Integer.toString(empireID));
        sitrep_pb.setReportTime(DateTime.now().getMillis() / 1000);
        sitrep_pb.setStarKey(star.getKey());
        sitrep_pb.setPlanetIndex(colony.getPlanetIndex());
        Messages.SituationReport.BuildCompleteRecord.Builder build_complete_pb = Messages.SituationReport.BuildCompleteRecord.newBuilder();
        build_complete_pb.setBuildKind(Messages.BuildRequest.BUILD_KIND.valueOf(designKind.getValue()));
        build_complete_pb.setBuildRequestKey(Integer.toString(buildRequestID));
        build_complete_pb.setDesignId(designID);
        sitrep_pb.setBuildCompleteRecord(build_complete_pb);
        if (star.getCombatReport() != null && fleet != null) {
            Messages.SituationReport.FleetUnderAttackRecord.Builder fleet_under_attack_pb = Messages.SituationReport.FleetUnderAttackRecord.newBuilder();
            fleet_under_attack_pb.setCombatReportKey(star.getCombatReport().getKey());
            fleet_under_attack_pb.setFleetDesignId(fleet.getDesignID());
            fleet_under_attack_pb.setFleetKey(fleet.getKey());
            fleet_under_attack_pb.setNumShips(fleet.getNumShips());
            sitrep_pb.setFleetUnderAttackRecord(fleet_under_attack_pb);
        }

        new SituationReportController().saveSituationReport(sitrep_pb.build());
    }

    private Fleet processFleetBuild(Star star, Colony colony, int empireID, String designID,
                                   float count) throws RequestException {
        Empire empire = new EmpireController().getEmpire(empireID);
        Fleet newFleet = new FleetController().createFleet(empire, star, designID, count);
        FleetMoveCompleteEvent.fireFleetArrivedEvents(star, newFleet);

        // todo: hard-coded?
        if (designID.equals("colonyship")) {
            // remove 100 population from the colony that built this guy
            new ColonyController().reducePopulation(colony, 100.0f);
        }

        return newFleet;
    }

    private void processBuildingBuild(Star star, Colony colony, int empireID, Integer existingBuildingID,
                                      String designID) throws RequestException {
        if (existingBuildingID == null) {
            new BuildingController().createBuilding(star, colony, designID);
        } else {
            new BuildingController().upgradeBuilding(star, colony, existingBuildingID);
        }
    }
}
