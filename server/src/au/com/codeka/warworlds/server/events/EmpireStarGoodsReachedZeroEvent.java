package au.com.codeka.warworlds.server.events;

import java.sql.ResultSet;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Event;
import au.com.codeka.warworlds.server.RequestContext;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.RealmController;
import au.com.codeka.warworlds.server.ctrl.StarController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.EmpirePresence;
import au.com.codeka.warworlds.server.model.Star;

public class EmpireStarGoodsReachedZeroEvent extends Event {
    private final Logger log = LoggerFactory.getLogger(BuildCompleteEvent.class);

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
            ResultSet rs = stmt.select();
            while (rs.next()) {
                int id = rs.getInt(1);
                int starID = rs.getInt(2);

                RequestContext.i.setContextName("event: EmpireStarGoodsReachedZero star.id="+starID);

                try {
                    Star star = new StarController().getStar(starID);
                    processStar(star);
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
            // TODO: errors?
        }
    }

    private void processStar(Star star) throws RequestException {
        Simulation sim = new Simulation();
        sim.simulate(star);

        for (BaseEmpirePresence baseEmpire : star.getEmpirePresences()) {
            EmpirePresence empire = (EmpirePresence) baseEmpire;
            if (empire.getTotalGoods() <= 0.0f) {
                Messages.SituationReport.Builder sitrep_pb = Messages.SituationReport.newBuilder();
                sitrep_pb.setRealm(new RealmController().getRealmName());
                sitrep_pb.setEmpireKey(empire.getEmpireKey());
                sitrep_pb.setReportTime(DateTime.now().getMillis() / 1000);
                sitrep_pb.setStarKey(star.getKey());
                sitrep_pb.setPlanetIndex(-1);
                Messages.SituationReport.StarRunOutOfGoodsRecord.Builder ran_out_of_goods_pb = Messages.SituationReport.StarRunOutOfGoodsRecord.newBuilder();
                for (BaseColony baseColony : star.getColonies()) {
                    Colony colony = (Colony) baseColony;
                    if (colony.getEmpireID() == empire.getEmpireID()) {
                        ran_out_of_goods_pb.setColonyKey(colony.getKey());
                        break;
                    }
                }
                sitrep_pb.setStarRanOutOfGoodsRecord(ran_out_of_goods_pb);

                //TODO
                //new SituationReportController().saveSituationReport(sitrep_pb.build());
            }
        }

        try {
            new StarController().update(star);
        } catch (MySQLTransactionRollbackException e) {
            throw new RequestException(e);
        }
    }
}
