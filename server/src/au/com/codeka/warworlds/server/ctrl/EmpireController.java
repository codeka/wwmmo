package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.sql.Statement;

import org.joda.time.DateTime;

import au.com.codeka.common.model.Simulation;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class EmpireController {
    private Simulation mSimulation;

    public EmpireController(Simulation sim) {
        mSimulation = sim;
    }

    public Empire getEmpireForUser(String userEmail) throws RequestException {
        return DataBase.getEmpireForUser(userEmail);
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

        // add the initial colony and fleets to the star
        sql = "INSERT INTO colonies (sector_id, star_id, planet_index, empire_id," +
                                   " focus_population, focus_construction, focus_farming," +
                                   " focus_mining, population, uncollected_taxes," +
                                   " cooldown_end_time)" +
             " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getSectorID());
            stmt.setInt(2, star.getID());
            stmt.setInt(3, starFinder.getPlanetIndex());
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

        // update the count of colonies in the sector
        sql = "UPDATE sectors SET num_colonies = num_colonies+1 WHERE id = ?";
        try (SqlStmt stmt = DB.prepare(sql)) {
            stmt.setInt(1, star.getSectorID());
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

    private static class DataBase {
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

        public static Empire getEmpireForUser(String userEmail) throws RequestException {
            String sql = "SELECT id, name, cash, home_star_id, user_email FROM empires WHERE user_email = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setString(1, userEmail);
                ResultSet rs = stmt.select();
                if (rs.next()) {
                    Empire empire = new Empire(rs);
                    empire.setHomeStar(new StarController().getStar(rs.getInt("home_star_id")));
                    return empire;
                }

                return null;
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }
    }
}
