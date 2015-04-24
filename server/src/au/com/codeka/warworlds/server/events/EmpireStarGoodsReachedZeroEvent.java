package au.com.codeka.warworlds.server.events;

import java.util.ArrayList;

import org.joda.time.DateTime;

import au.com.codeka.common.Log;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.SituationReport;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.Event;
import au.com.codeka.warworlds.server.RequestContext;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.SituationReportController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.EmpirePresence;
import au.com.codeka.warworlds.server.model.Star;

public class EmpireStarGoodsReachedZeroEvent extends Event {
    private final Log log = new Log("BuildCompleteEvent");

    @Override
    public String getNextEventTimeSql() {
        return "SELECT MIN(goods_zero_time) FROM empire_presences";
    }

    @Override
    public void process() {
        ArrayList<Integer> processedIDs = new ArrayList<Integer>();
        String sql = "SELECT id, star_id" +
                    " FROM empire_presences" +
                    " WHERE goods_zero_time IS NOT NULL AND goods_zero_time < ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, DateTime.now().plusSeconds(10));
            SqlResult res = stmt.select();
            while (res.next()) {
                int id = res.getInt(1);
                int starID = res.getInt(2);

                RequestContext.i.setContext("event: EmpireStarGoodsReachedZero star.id="+starID);

                try {
                    Star star = new StarController().getStar(starID);
                    processStar(star);
                } catch (Exception e) {
                    log.error("Error processing goods-zero event!", e);
                }

                processedIDs.add(id);
            }
        } catch(Exception e) {
            log.error("Error processing goods-zero event!", e);
        }

        if (processedIDs.isEmpty()) {
            return;
        }

        sql = "UPDATE empire_presences SET goods_zero_time = NULL WHERE id IN ";
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
            log.error("Error processing empire-star-goods-zero event!", e);
        }
    }

    private void processStar(Star star) throws RequestException {
        Simulation sim = new Simulation();
        sim.simulate(star);

        for (BaseEmpirePresence baseEmpire : star.getEmpirePresences()) {
            EmpirePresence empire = (EmpirePresence) baseEmpire;
            if (empire.getTotalGoods() <= 0.0f) {
                SituationReport.Builder sitrep_pb = new SituationReport.Builder();
                sitrep_pb.realm = Configuration.i.getRealmName();
                sitrep_pb.empire_key = empire.getEmpireKey();
                sitrep_pb.report_time = DateTime.now().getMillis() / 1000;
                sitrep_pb.star_key = star.getKey();
                sitrep_pb.planet_index = -1;
                SituationReport.StarRunOutOfGoodsRecord.Builder ran_out_of_goods_pb =
                    new SituationReport.StarRunOutOfGoodsRecord.Builder();
                for (BaseColony baseColony : star.getColonies()) {
                    Colony colony = (Colony) baseColony;
                    if (colony.getEmpireID() != null && colony.getEmpireID() == empire.getEmpireID()) {
                        ran_out_of_goods_pb.colony_key = colony.getKey();
                        break;
                    }
                }
                sitrep_pb.star_ran_out_of_goods_record = ran_out_of_goods_pb.build();

                new SituationReportController().saveSituationReport(sitrep_pb.build());
            }
        }

        new StarController().update(star);
    }
}
