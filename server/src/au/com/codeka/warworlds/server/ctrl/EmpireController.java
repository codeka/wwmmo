package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class EmpireController {
    public Empire getEmpire(int id) throws RequestException {
        return DataBase.getEmpires(new int[] {id}).get(0);
    }

    public List<Empire> getEmpires(int[] ids) throws RequestException {
        return DataBase.getEmpires(ids);
    }

    public Empire getEmpireByEmail(String email) throws RequestException {
        return DataBase.getEmpireByEmail(email);
    }

    public List<Empire> getEmpiresByName(String name, int limit) throws RequestException {
        return DataBase.getEmpiresByName(name, limit);
    }

    public int[] getStarsForEmpire(int empireId) throws RequestException {
        return DataBase.getStarsForEmpire(empireId);
    }

    public boolean withdrawCash(int empireId, float amount, Messages.CashAuditRecord.Builder audit_record_pb) throws RequestException {
        try (Transaction t = DB.beginTransaction()) {
            SqlStmt stmt = t.prepare("SELECT cash FROM empires WHERE id = ?");
            stmt.setInt(1, empireId);
            double cashBefore = stmt.selectFirstValue(Double.class);
            if (cashBefore <= amount) {
                return false;
            }

            audit_record_pb.setBeforeCash((float) cashBefore);
            audit_record_pb.setAfterCash((float) (cashBefore - amount));

            stmt = t.prepare("UPDATE empires SET cash = cash - ? WHERE id = ?");
            stmt.setDouble(1, amount);
            stmt.setInt(2, empireId);
            stmt.update();

            stmt = t.prepare("INSERT INTO empire_cash_audit (empire_id, cash_before, cash_after," +
                                                           " time, reason) VALUES (?, ?, ?, ?, ?)");
            stmt.setInt(1, empireId);
            stmt.setDouble(2, cashBefore);
            stmt.setDouble(3, cashBefore - amount);
            stmt.setDateTime(4, DateTime.now());
            stmt.setBlob(5, audit_record_pb.build().toByteArray());
            stmt.update();

            t.commit();
            return true;
        } catch(Exception e) {
            throw new RequestException(500, e);
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
        DataBase.createEmpire(empire);

        // empty the star of it's current (native) inhabitants
        String sql = "DELETE FROM colonies WHERE star_id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getID());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }
        sql = "DELETE FROM fleets WHERE star_id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getID());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }

        colonize(empire, star, starFinder.getPlanetIndex());

        sql = "INSERT INTO empire_presences (empire_id, star_id, total_goods, total_minerals) VALUES (?, ?, ?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, empire.getID());
            stmt.setInt(2, star.getID());
            stmt.setDouble(3, 500.0);
            stmt.setDouble(4, 500.0);
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }

        sql = "INSERT INTO fleets (sector_id, star_id, design_name, empire_id," +
                                 " num_ships, stance, state, state_start_time)" +
             " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getSectorID());
            stmt.setInt(2, star.getID());
            stmt.setString(3, "colonyship");
            stmt.setInt(4, empire.getID());
            stmt.setDouble(5, 1.0);
            stmt.setInt(6, Fleet.Stance.AGGRESSIVE.getValue());
            stmt.setInt(7, Fleet.State.IDLE.getValue());
            stmt.setDateTime(8, DateTime.now());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }
        sql = "INSERT INTO fleets (sector_id, star_id, design_name, empire_id," +
                                 " num_ships, stance, state, state_start_time)" +
             " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getSectorID());
            stmt.setInt(2, star.getID());
            stmt.setString(3, "scout");
            stmt.setInt(4, empire.getID());
            stmt.setDouble(5, 10.0);
            stmt.setInt(6, Fleet.Stance.AGGRESSIVE.getValue());
            stmt.setInt(7, Fleet.State.IDLE.getValue());
            stmt.setDateTime(8, DateTime.now());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }

        // update the last simulation time for the star so that it doesn't simulate until we
        // actually arrived...
        sql = "UPDATE stars SET last_simulation = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setDateTime(1, DateTime.now());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }
    }

    private void colonize(Empire empire, Star star, int planetIndex) throws RequestException {
        // add the initial colony and fleets to the star
        String sql = "INSERT INTO colonies (sector_id, star_id, planet_index, empire_id," +
                                          " focus_population, focus_construction, focus_farming," +
                                          " focus_mining, population, uncollected_taxes," +
                                          " cooldown_end_time)" +
             " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getSectorID());
            stmt.setInt(2, star.getID());
            stmt.setInt(3, planetIndex);
            stmt.setInt(4, empire.getID());
            stmt.setDouble(5, 0.25);
            stmt.setDouble(6, 0.25);
            stmt.setDouble(7, 0.25);
            stmt.setDouble(8, 0.25);
            stmt.setDouble(9, 100.0);
            stmt.setDouble(10, 0.0);
            stmt.setDateTime(11, DateTime.now().plusHours(8));
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }

        // update the count of colonies in the sector
        sql = "UPDATE sectors SET num_colonies = num_colonies+1 WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getSectorID());
            stmt.update();
        } catch(Exception e) {
            throw new RequestException(500, e);
        }
    }

    private static class DataBase extends BaseDataBase {
        public static void createEmpire(Empire empire) throws RequestException {
            String sql = "INSERT INTO empires (name, cash, home_star_id, user_email) VALUES (?, ?, ?, ?)";
            try (SqlStmt stmt = DB.prepare(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, empire.getDisplayName());
                stmt.setDouble(2, 500.0);
                stmt.setInt(3, ((Star) empire.getHomeStar()).getID());
                stmt.setString(4, empire.getEmailAddr());
                stmt.update();
                empire.setID(stmt.getAutoGeneratedID());
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }

        public static List<Empire> getEmpires(int[] ids) throws RequestException {
            String sql = "SELECT * FROM empires WHERE id IN "+buildInClause(ids);

            try (SqlStmt stmt = DB.prepare(sql)) {
                ResultSet rs = stmt.select();

                ArrayList<Empire> empires = new ArrayList<Empire>();
                while (rs.next()) {
                    empires.add(new Empire(rs));
                }
                populateEmpires(empires);
                return empires;
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }

        public static Empire getEmpireByEmail(String email) throws RequestException {
            String sql = "SELECT * FROM empires WHERE user_email = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setString(1, email);
                ResultSet rs = stmt.select();

                ArrayList<Empire> empires = new ArrayList<Empire>();
                if (rs.next()) {
                    empires.add(new Empire(rs));
                }
                if (empires.size() == 0) {
                    return null;
                }

                populateEmpires(empires);
                return empires.get(0);
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }

        public static List<Empire> getEmpiresByName(String name, int limit) throws RequestException {
            String sql = "SELECT * FROM empires WHERE name LIKE ? LIMIT ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setString(1, "%"+name+"%"); // TODO: escape?
                stmt.setInt(2, limit);
                ResultSet rs = stmt.select();

                ArrayList<Empire> empires = new ArrayList<Empire>();
                if (rs.next()) {
                    empires.add(new Empire(rs));
                }

                populateEmpires(empires);
                return empires;
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }

        private static void populateEmpires(List<Empire> empires) throws RequestException {
            for (Empire empire : empires) {
                empire.setHomeStar(new StarController().getStar(empire.getHomeStarID()));
            }
        }

        private static int[] getStarsForEmpire(int empireId) throws RequestException {
            String sql = "SELECT star_id FROM fleets WHERE empire_id = ?" +
                        " UNION SELECT star_id FROM colonies WHERE empire_id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, empireId);
                stmt.setInt(2, empireId);
                ResultSet rs = stmt.select();

                ArrayList<Integer> starIds = new ArrayList<Integer>();
                while (rs.next()) {
                    starIds.add(rs.getInt(1));
                }

                int[] array = new int[starIds.size()];
                for (int i = 0; i < array.length; i++) {
                    array[i] = starIds.get(i);
                }
                return array;
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }
    }
}
