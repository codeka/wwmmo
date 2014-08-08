package au.com.codeka.warworlds.server.events;

import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Event;
import au.com.codeka.warworlds.server.RequestContext;
import au.com.codeka.warworlds.server.ctrl.RealmController;
import au.com.codeka.warworlds.server.ctrl.SituationReportController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.CombatReport;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class FleetDestroyedEvent extends Event {
    private final Logger log = LoggerFactory.getLogger(FleetDestroyedEvent.class);

    @Override
    protected String getNextEventTimeSql() {
        return "SELECT MIN(time_destroyed) FROM fleets";
    }

    @Override
    public void process() {
        String sql = "SELECT id, star_id FROM fleets WHERE time_destroyed < ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, DateTime.now().plusSeconds(10)); // anything in the next 10 seconds is a candidate
            SqlResult res = stmt.select();
            while (res.next()) {
                int fleetID = res.getInt(1);
                Star star = new StarController().getStar(res.getInt(2));

                RequestContext.i.setContext("event: FleetDestroyedEvent fleet.id="+fleetID);

                try {
                    processFleetDestroyed(star, fleetID);
                } catch (Exception e) {
                    log.error("Error processing fleet-move event!", e);
                }
            }
        } catch(Exception e) {
            log.error("Error processing fleet-move event!", e);
        }
    }

    private void processFleetDestroyed(Star star, int fleetID) throws Exception {
        Fleet fleet = (Fleet) star.findFleet(Integer.toString(fleetID));
        if (fleet == null) {
            return;
        }

        Simulation sim = new Simulation();
        sim.simulate(star);
        new StarController().update(star);

        // if the fleet is no longer in the star, then it was destroyed!
        boolean fleetWasDestroyed = true;
        for (BaseFleet baseFleet : star.getFleets()) {
            Fleet f = (Fleet) baseFleet;
            if (f.getID() == fleetID) {
                fleetWasDestroyed = false;
            }
        }

        CombatReport combatReport = (CombatReport) star.getCombatReport();
        if (fleetWasDestroyed && fleet.getEmpireKey() != null) {
            // send a notification that this fleet was destroyed
            Messages.SituationReport.Builder sitrep_pb = Messages.SituationReport.newBuilder();
            sitrep_pb.setRealm(new RealmController().getRealmName());
            sitrep_pb.setEmpireKey(fleet.getEmpireKey());
            sitrep_pb.setReportTime(DateTime.now().getMillis() / 1000);
            sitrep_pb.setStarKey(star.getKey());
            sitrep_pb.setPlanetIndex(-1);
            Messages.SituationReport.FleetDestroyedRecord.Builder fleet_destroyed_pb = Messages.SituationReport.FleetDestroyedRecord.newBuilder();
            if (combatReport != null) {
                fleet_destroyed_pb.setCombatReportKey(combatReport.getKey());
            }
            fleet_destroyed_pb.setFleetDesignId(fleet.getDesignID());
            sitrep_pb.setFleetDestroyedRecord(fleet_destroyed_pb);
            new SituationReportController().saveSituationReport(sitrep_pb.build());
        }

        // if there's a combat report
        if (combatReport != null) {
            List<CombatReport.CombatRound> rounds = combatReport.getCombatRounds();
            if (rounds.size() >= 1) {
                CombatReport.CombatRound lastRound = rounds.get(rounds.size() - 1);
                int numDestroyedFleetEmpireFleets = 0;
                String nonDestroyedEmpireKey = null;
                boolean onlyOneOtherEmpire = true;
                CombatReport.FleetSummary victoriousFleetSummary = null;
                for (CombatReport.FleetSummary fleetSummary : lastRound.getFleets()) {
                    if ((fleetSummary.getEmpireKey() == null && fleet.getEmpireKey() == null) ||
                            (fleetSummary.getEmpireKey() != null && fleet.getEmpireKey() != null && fleetSummary.getEmpireKey().equals(fleet.getEmpireKey()))) {
                        numDestroyedFleetEmpireFleets ++;
                    } else {
                        if (nonDestroyedEmpireKey == null ||
                                nonDestroyedEmpireKey.equals(fleetSummary.getEmpireKey())) {
                            nonDestroyedEmpireKey = fleetSummary.getEmpireKey();
                            victoriousFleetSummary = fleetSummary;
                        } else {
                            onlyOneOtherEmpire = false;
                        }
                    }
                }

                if (onlyOneOtherEmpire && numDestroyedFleetEmpireFleets == 1 &&
                        victoriousFleetSummary != null &&
                        victoriousFleetSummary.getEmpireKey() != null &&
                        !victoriousFleetSummary.getEmpireKey().isEmpty()) {
                    // if there's only one other empire, and only one fleet from the empire
                    // that was just destroyed, it means the other empire was victorious!
                    Messages.SituationReport.Builder sitrep_pb = Messages.SituationReport.newBuilder();
                    sitrep_pb.setRealm(new RealmController().getRealmName());
                    sitrep_pb.setEmpireKey(victoriousFleetSummary.getEmpireKey());
                    sitrep_pb.setReportTime(DateTime.now().getMillis() / 1000);
                    sitrep_pb.setStarKey(star.getKey());
                    sitrep_pb.setPlanetIndex(-1);
                    Messages.SituationReport.FleetVictoriousRecord.Builder fleet_victorious_pb = Messages.SituationReport.FleetVictoriousRecord.newBuilder();
                    fleet_victorious_pb.setCombatReportKey(combatReport.getKey());
                    fleet_victorious_pb.setFleetDesignId(victoriousFleetSummary.getDesignID()); // TODO: more than one?
                    fleet_victorious_pb.setFleetKey(victoriousFleetSummary.getFleetKeys().get(0)); // TODO: more than one!
                    fleet_victorious_pb.setNumShips(victoriousFleetSummary.getNumShips());
                    sitrep_pb.setFleetVictoriousRecord(fleet_victorious_pb);
                    new SituationReportController().saveSituationReport(sitrep_pb.build());
                }
            }
        }
    }
}
