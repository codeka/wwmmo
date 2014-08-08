package au.com.codeka.warworlds.server.ctrl;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joda.time.DateTime;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.AllianceRequest;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Planet;
import au.com.codeka.warworlds.server.model.Star;

public class EmpireController {
    private DataBase db;

    public EmpireController() {
        db = new DataBase();
    }
    public EmpireController(Transaction trans) {
        db = new DataBase(trans);
    }

    public Empire getEmpire(int id) throws RequestException {
        try {
            List<Empire> empires = db.getEmpires(new int[] {id});
            if (empires.size() == 0) {
                return null;
            }
            return empires.get(0);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public List<Empire> getEmpires(int[] ids) throws RequestException {
        try {
            return db.getEmpires(ids);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public Map<Integer, Double> getTaxCollectedPerHour(Collection<Integer> empireIDs) throws RequestException {
        try {
            return db.getTaxCollectedPerHour(empireIDs);
        } catch (Exception e) {
            throw new RequestException(e);
        }

    }

    public Empire getEmpireByEmail(String email) throws RequestException {
        try {
            return db.getEmpireByEmail(email);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public List<Empire> getEmpiresByName(String name, int limit) throws RequestException {
        try {
            return db.getEmpiresByName(name, limit);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public List<Empire> getEmpiresByRank(int minRank, int maxRank) throws RequestException {
        try {
            return db.getEmpiresByRank(minRank, maxRank);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    /** Marks an empire active, that was previously marked abandoned. */
    public void markActive(Empire empire) throws RequestException {
        try (SqlStmt stmt = db.prepare("UPDATE empires SET state = ? WHERE id = ? AND state = ?")) {
            stmt.setInt(1, Empire.State.ACTIVE.getValue());
            stmt.setInt(2, empire.getID());
            stmt.setInt(3, Empire.State.ABANDONED.getValue());
            stmt.update();
        } catch (Exception e) {
            throw new RequestException(e);
        }

        // TODO: remove the empire's stars from the "abandoned stars" list...
    }

    public ArrayList<Integer> getStarsForEmpire(int empireId, EmpireStarsFilter filter,
            String search) throws RequestException {
        try {
            return db.getStarsForEmpire(empireId, filter, search);
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public void update(Empire empire) throws RequestException {
        try (SqlStmt stmt = db.prepare("UPDATE empires SET name = ? WHERE id = ?")) {
            stmt.setString(1, empire.getDisplayName());
            stmt.setInt(2, empire.getID());
            stmt.update();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public void changeEmpireShield(int empireID, byte[] pngImage) throws RequestException {
        String sql = "INSERT INTO empire_shields (empire_id, create_date, rejected, image) VALUES (?, NOW(), 0, ?)";
        try (SqlStmt stmt = db.prepare(sql)) {
            stmt.setInt(1, empireID);
            stmt.setBytes(2, pngImage);
            stmt.update();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    public byte[] getEmpireShield(int empireID, Integer shieldID) throws RequestException {
        String sql = "SELECT image FROM empire_shields " +
                    " WHERE empire_id = ? AND rejected = 0 ";
        if (shieldID != null) {
            sql += " AND id = ?";
        }
        sql += " ORDER BY create_date DESC LIMIT 1";
        try (SqlStmt stmt = db.prepare(sql)) {
            stmt.setInt(1, empireID);
            if (shieldID != null) {
                stmt.setInt(2, shieldID);
            }
            SqlResult res = stmt.select();
            if (res.next()) {
                return res.getBytes(1);
            }
        } catch (Exception e) {
            throw new RequestException(e);
        }

        return null;
    }

    public boolean withdrawCash(int empireId, float amount, Messages.CashAuditRecord.Builder audit_record_pb) throws RequestException {
        return adjustBalance(empireId, -amount, audit_record_pb);
    }

    public void depositCash(int empireId, float amount, Messages.CashAuditRecord.Builder audit_record_pb) throws RequestException {
        adjustBalance(empireId, amount, audit_record_pb);
    }

    public boolean adjustBalance(int empireId, float amount, Messages.CashAuditRecord.Builder audit_record_pb) throws RequestException {
        if (Float.isNaN(amount)) {
            throw new RequestException(500, "Amount is NaN!");
        }
        if (Float.isInfinite(amount)) {
            throw new RequestException(500, "Amount is Infinite!");
        }

        Transaction t = db.getTransaction();
        boolean existingTransaction = (t != null);
        if (t == null) {
            try {
                t = DB.beginTransaction();
            } catch (SQLException e) {
                throw new RequestException(e);
            }
        }

        try {
            SqlStmt stmt = t.prepare("SELECT cash FROM empires WHERE id = ?");
            stmt.setInt(1, empireId);
            double cashBefore = stmt.selectFirstValue(Double.class);
            if (amount < 0 && cashBefore <= Math.abs(amount)) {
                return false;
            }

            audit_record_pb.setBeforeCash((float) cashBefore);
            audit_record_pb.setAfterCash((float) (cashBefore + amount));
            audit_record_pb.setTime(DateTime.now().getMillis() / 1000);

            stmt = t.prepare("UPDATE empires SET cash = cash + ? WHERE id = ?");
            stmt.setDouble(1, amount);
            stmt.setInt(2, empireId);
            stmt.update();

            stmt = t.prepare("INSERT INTO empire_cash_audit (empire_id, cash_before, cash_after," +
                                                           " time, reason) VALUES (?, ?, ?, ?, ?)");
            stmt.setInt(1, empireId);
            stmt.setDouble(2, cashBefore);
            stmt.setDouble(3, cashBefore - amount);
            stmt.setDateTime(4, DateTime.now());
            stmt.setBytes(5, audit_record_pb.build().toByteArray());
            stmt.update();

            if (!existingTransaction) {
                t.commit();
            }
            return true;
        } catch(Exception e) {
            throw new RequestException(e);
        } finally {
            if (!existingTransaction) {
                try {
                    t.close();
                } catch (Exception e) {
                    throw new RequestException(e);
                }
            }
        }
    }

    public void createEmpire(Empire empire) throws RequestException {
        if (empire.getDisplayName().trim().equals("")) {
            throw new RequestException(400, Messages.GenericError.ErrorCode.CannotCreateEmpireBlankName,
                                       "You must give your empire a name.");
        }

        NewEmpireStarFinder starFinder = new NewEmpireStarFinder();
        if (!starFinder.findStarForNewEmpire()) {
            throw new RequestException(500); // todo: expand universe
        }
        Star star = new StarController().getStar(starFinder.getStarID());
        empire.setHomeStar(star);

        // create the empire
        db.createEmpire(empire);

        // empty the star of it's current (native) inhabitants
        String sql = "DELETE FROM colonies WHERE star_id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getID());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }
        sql = "DELETE FROM fleets WHERE star_id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getID());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }
        sql = "UPDATE stars SET time_emptied = ? WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, DateTime.now());
            stmt.setInt(2, star.getID());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }

        // re-fetch the star with it's new details...
        star = new StarController().getStar(star.getID());
        Planet planet = (Planet) star.getPlanets()[starFinder.getPlanetIndex() - 1];

        new ColonyController(db.getTransaction()).colonize(empire, star, starFinder.getPlanetIndex(),
                planet.getPopulationCongeniality() * 0.8f);

        new FleetController(db.getTransaction()).createFleet(empire, star, "colonyship", 2.0f);
        new FleetController(db.getTransaction()).createFleet(empire, star, "scout", 10.0f);
        new FleetController(db.getTransaction()).createFleet(empire, star, "fighter", 50.0f);
        new FleetController(db.getTransaction()).createFleet(empire, star, "troopcarrier", 150.0f);

        // update the last simulation time for the star so that it doesn't simulate until we
        // actually arrived...
        star.setLastSimulation(DateTime.now());
        new StarController().update(star);
    }

    /**
     * Resets the given empire. This is obviously fairly destructive, so be careful!
     */
    public void resetEmpire(int empireID, String resetReason) throws RequestException {
        String[] sqls = {
                "DELETE FROM alliance_join_requests WHERE empire_id = ?",
                "DELETE FROM build_requests WHERE empire_id = ?",
                "DELETE FROM buildings WHERE empire_id = ?",
                "DELETE FROM colonies WHERE empire_id = ?",
                "DELETE FROM empire_presences WHERE empire_id = ?",
                "DELETE FROM fleets WHERE empire_id = ?",
                "DELETE FROM scout_reports WHERE empire_id = ?",
                "DELETE FROM situation_reports WHERE empire_id = ?",
            };

        try (Transaction t = DB.beginTransaction()) {
            for (String sql : sqls) {
                try (SqlStmt stmt = t.prepare(sql)) {
                    stmt.setInt(1, empireID);
                    stmt.update();
                }
            }

            String sql = "UPDATE empires SET alliance_id = NULL, cash = 2000, reset_reason = ? WHERE id = ?";
            try (SqlStmt stmt = t.prepare(sql)) {
                stmt.setString(1, resetReason);
                stmt.setInt(2, empireID);
                stmt.update();
            }

            t.commit();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }

    /**
     * Gets the reason (if any) for the resetting of the given empire. Once we're returned this, we'll
     * reset the reason back to NULL.
     */
    public String getResetReason(int empireID) throws RequestException{
        String reason = null;

        // empty the star of it's current (native) inhabitants
        String sql = "SELECT reset_reason FROM empires WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, empireID);
            reason = stmt.selectFirstValue(String.class);
        } catch(Exception e) {
            throw new RequestException(e);
        }

        sql = "UPDATE empires SET reset_reason = NULL WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, empireID);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }

        return reason;
    }

    /**
     * If your old home star has been destroyed, for example, this will find us a new one.
     */
    public void findNewHomeStar(int empireID) throws RequestException {
        ArrayList<Integer> starIds = new ArrayList<Integer>();
        String sql = "SELECT DISTINCT star_id" +
                    " FROM colonies" +
                    " WHERE empire_id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, empireID);
            SqlResult res = stmt.select();
            while (res.next()) {
                starIds.add(res.getInt(1));
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }

        // find the star with the biggest population, that'll be our new home.
        Star bestStar = null;
        float bestStarPopulation = 0;
        for (Star star : new StarController().getStars(starIds)) {
            if (bestStar == null) {
                bestStar = star;
                bestStarPopulation = getTotalPopulation(star, empireID);
            } else {
                float thisStarPopulation = getTotalPopulation(star, empireID);
                if (thisStarPopulation > bestStarPopulation) {
                    bestStar = star;
                    bestStarPopulation = thisStarPopulation;
                }
            }
        }

        if (bestStar != null) {
            setHomeStar(empireID, bestStar.getID());
        }
    }

    public void setHomeStar(int empireID, int starID) throws RequestException {
        String sql = "UPDATE empires SET home_star_id = ? WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, starID);
            stmt.setInt(2, empireID);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    private static float getTotalPopulation(Star star, int empireID) {
        float population = 0;
        for (BaseColony baseColony : star.getColonies()) {
            Colony colony = (Colony) baseColony;
            if (colony.getEmpireID() != null && colony.getEmpireID() == empireID) {
                population += colony.getPopulation();
            }
        }
        return population;
    }

    private static class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction trans) {
            super(trans);
        }

        public void createEmpire(Empire empire) throws RequestException {
            String sql;
            if (empire.getKey() == null || empire.getID() == 0) {
                sql = "INSERT INTO empires (name, cash, home_star_id, user_email, signup_date) VALUES (?, ?, ?, ?, NOW())";
            } else {
                sql = "UPDATE empires SET name = ?, cash = ?, home_star_id = ?, user_email = ? WHERE id = ?";
            }
            try (SqlStmt stmt = prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, empire.getDisplayName());
                stmt.setDouble(2, 2000.0);
                stmt.setInt(3, ((Star) empire.getHomeStar()).getID());
                stmt.setString(4, empire.getEmailAddr());
                if (empire.getKey() != null && empire.getID() != 0) {
                    stmt.setInt(5, empire.getID());
                }
                stmt.update();
                if (empire.getKey() == null || empire.getID() == 0) {
                    empire.setID(stmt.getAutoGeneratedID());
                }
            } catch(SQLException e) {
                if (DB.isConstraintViolation(e)) {
                    // this can actually be one of two things, either the empire name is already taken,
                    // or the user's email address is not actually unique. The former is far more likey
                    // than the latter, though
                    throw new RequestException(400, Messages.GenericError.ErrorCode.EmpireNameExists,
                            String.format("The empire name you've chosen, '%s' already exists. Please choose a different name.", empire.getDisplayName()));
                } else {
                    throw new RequestException(e);
                }
            } catch(Exception e) {
                throw new RequestException( e);
            }
        }

        public List<Empire> getEmpires(int[] ids) throws Exception {
            String sql = getSelectEmpire("empires.id IN "+buildInClause(ids), true);

            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                ArrayList<Empire> empires = new ArrayList<Empire>();
                while (res.next()) {
                    empires.add(new Empire(res));
                }
                populateEmpires(empires);
                return empires;
            }
        }

        public Map<Integer, Double> getTaxCollectedPerHour(Collection<Integer> empireIDs) throws Exception {
            String sql = "SELECT empire_id, sum(tax_per_hour)" +
                        " FROM empire_presences" +
                        " WHERE empire_id IN " + buildInClause(empireIDs) +
                        " GROUP BY empire_id";

            try (SqlStmt stmt = prepare(sql)) {
                SqlResult res = stmt.select();

                Map<Integer, Double> taxRates = new TreeMap<Integer, Double>();
                while (res.next()) {
                    taxRates.put(res.getInt(1), res.getDouble(2));
                }
                return taxRates;
            }
        }

        public Empire getEmpireByEmail(String email) throws Exception {
            String sql = getSelectEmpire("user_email = ?", true);
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setString(1, email);
                SqlResult res = stmt.select();

                ArrayList<Empire> empires = new ArrayList<Empire>();
                if (res.next()) {
                    empires.add(new Empire(res));
                }
                if (empires.size() == 0) {
                    return null;
                }

                populateEmpires(empires);
                return empires.get(0);
            }
        }

        public List<Empire> getEmpiresByName(String name, int limit) throws Exception {
            String sql = getSelectEmpire("empires.name ~* ? LIMIT ?", false);
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setString(1, name);
                stmt.setInt(2, limit);
                SqlResult res = stmt.select();

                ArrayList<Empire> empires = new ArrayList<Empire>();
                while (res.next()) {
                    empires.add(new Empire(res));
                }

                populateEmpires(empires);
                return empires;
            }
        }

        public List<Empire> getEmpiresByRank(int minRank, int maxRank) throws Exception {
            String sql = getSelectEmpire("empires.id IN (SELECT empire_id FROM empire_ranks WHERE rank BETWEEN ? AND ?)", false);
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, minRank);
                stmt.setInt(2, maxRank);
                SqlResult res = stmt.select();

                ArrayList<Empire> empires = new ArrayList<Empire>();
                while (res.next()) {
                    empires.add(new Empire(res));
                }

                populateEmpires(empires);
                return empires;
            }
        }

        private String getSelectEmpire(String whereClause, boolean includeBanned) {
            String sql = "SELECT *, alliances.id AS alliance_id, alliances.name as alliance_name," +
                               " (SELECT COUNT(*) FROM empires WHERE alliance_id = empires.alliance_id) AS num_empires," +
                               " (SELECT MAX(create_date) FROM empire_shields WHERE empire_shields.empire_id = empires.id AND rejected = 0) AS shield_last_update," +
                               " (SELECT COUNT(*) FROM alliance_requests WHERE alliance_id = alliances.id AND state = " + AllianceRequest.RequestState.PENDING.getNumber() + ") AS num_pending_requests" +
                         " FROM empires" +
                         " LEFT JOIN alliances ON empires.alliance_id = alliances.id" +
                         " LEFT JOIN empire_ranks ON empires.id = empire_ranks.empire_id" +
                         " WHERE ";
            if (!includeBanned) {
                sql += "state != 2 AND ";
            }
            sql += whereClause;
            return sql;
        }

        private void populateEmpires(List<Empire> empires) throws SQLException {
            for (Empire empire : empires) {
                try {
                    empire.setHomeStar(new StarController().getStar(empire.getHomeStarID()));
                } catch (RequestException e) {
                    // TODO: ignore??
                }
            }
        }

        private ArrayList<Integer> getStarsForEmpire(int empireId, EmpireStarsFilter filter,
                String search) throws Exception {
            String sql;
            int numEmpireIds = 1;
            switch (filter) {
            case Colonies:
                sql = "SELECT id FROM stars WHERE id IN (" +
                       " SELECT star_id FROM colonies WHERE empire_id = ?" +
                      ")";
                break;
            case Fleets:
                sql = "SELECT id FROM stars WHERE id IN (" +
                        " SELECT star_id FROM fleets WHERE empire_id = ?" +
                       ")";
                break;
            case Building:
                sql = "SELECT id FROM beta.stars WHERE id IN (" +
                       " SELECT star_id FROM build_requests WHERE empire_id = ?" +
                      ")";
                break;
            case NotBuilding:
                sql = "SELECT id FROM stars WHERE id NOT IN (" +
                       " SELECT star_id FROM build_requests WHERE empire_id = ?" +
                      ") AND id IN (" +
                       " SELECT star_id FROM colonies WHERE empire_id = ?" +
                      ")";
                numEmpireIds = 2;
                break;
            default: // case Everything:
                sql = "SELECT id FROM stars" +
                      " INNER JOIN (" +
                        " SELECT star_id FROM fleets WHERE empire_id = ?" +
                        " UNION SELECT star_id FROM colonies WHERE empire_id = ?" +
                       ") AS ids ON ids.star_id = stars.id";
                numEmpireIds = 2;
                break;
            }
            if (search != null) {
                sql += " AND name ~* ?";
            }
            sql += " ORDER BY name";
            try (SqlStmt stmt = prepare(sql)) {
                for (int i = 0; i < numEmpireIds; i++ ) {
                    stmt.setInt(i + 1, empireId);
                }
                if (search != null) {
                    stmt.setString(numEmpireIds + 1, search);
                }
                SqlResult res = stmt.select();

                ArrayList<Integer> starIds = new ArrayList<Integer>();
                while (res.next()) {
                    starIds.add(res.getInt(1));
                }

                return starIds;
            }
        }
    }

    public enum EmpireStarsFilter {
        Everything,
        Colonies,
        Fleets,
        Building,
        NotBuilding
    }
}
