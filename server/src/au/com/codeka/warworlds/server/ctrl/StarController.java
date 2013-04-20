package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.util.ArrayList;

import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.EmpirePresence;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.Star;

public class StarController {

    public Star getStar(int id) throws RequestException {
        return DataBase.getStar(id);
    }

    private static class DataBase {
        public static Star getStar(int id) throws RequestException {
            final String sql = "SELECT stars.id, sector_id, name, sectors.x AS sector_x," +
                                     " sectors.y AS sector_y, stars.x, stars.y, size, star_type, planets," +
                                     " last_simulation, time_emptied" +
                              " FROM stars" +
                              " INNER JOIN sectors ON stars.sector_id = sectors.id" +
                              " WHERE stars.id = ?";

            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.select();

                if (rs.next()) {
                    Star star = new Star(rs);
                    populateStar(star);
                    return star;
                }

                throw new RequestException(404);
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }

        private static void populateStar(Star star) throws RequestException {
            String sql = "SELECT colonies.*" +
                        " FROM colonies" +
                        " WHERE star_id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, star.getID());
                ResultSet rs = stmt.select();

                ArrayList<BaseColony> colonies = new ArrayList<BaseColony>();
                while (rs.next()) {
                    colonies.add(new Colony(rs));
                }
                star.setColonies(colonies);
            } catch(Exception e) {
                throw new RequestException(500, e);
            }

            sql = "SELECT fleets.*" +
                 " FROM fleets" +
                 " WHERE star_id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, star.getID());
                ResultSet rs = stmt.select();

                ArrayList<BaseFleet> fleets = new ArrayList<BaseFleet>();
                while (rs.next()) {
                    fleets.add(new Fleet(rs));
                }
                star.setFleets(fleets);
            } catch(Exception e) {
                throw new RequestException(500, e);
            }

            sql = "SELECT empire_presences.*" +
                 " FROM empire_presences" +
                 " WHERE empire_presences.star_id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, star.getID());
                ResultSet rs = stmt.select();

                ArrayList<BaseEmpirePresence> empires = new ArrayList<BaseEmpirePresence>();
                while (rs.next()) {
                    empires.add(new EmpirePresence(rs));
                }
                star.setEmpires(empires);
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }
    }
}
