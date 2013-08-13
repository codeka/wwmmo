package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;

public class CombatReportController {
    private DataBase db;

    public CombatReportController() {
        db = new DataBase();
    }
    public CombatReportController(Transaction trans) {
        db = new DataBase(trans);
    }

    public Messages.CombatReport fetchCombatReportPb(int combatReportID) throws RequestException {
        try {
            return db.fetchCombatReportPb(combatReportID);
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    /**
     * Purges (deletes) all combat reports from the database older than the specified date.
     * Essentially, there's little point having combat reports from 6 months ago, and really they
     * just inflate the database.
     */
    public void purgeCombatReportsOlderThan(DateTime dt) throws Exception {
        String sql = "DELETE FROM combat_reports WHERE end_time < ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, dt);
            stmt.update();
        }
    }

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public Messages.CombatReport fetchCombatReportPb(int combatReportID) throws Exception {
            String sql = "SELECT rounds FROM combat_reports WHERE id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, combatReportID);
                ResultSet rs = stmt.select();
                while (rs.next()) {
                    return Messages.CombatReport.parseFrom(rs.getBytes(1));
                }
            }

            return null;
        }

    }
}
