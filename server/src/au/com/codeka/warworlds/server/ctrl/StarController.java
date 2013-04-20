package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
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
                    // todo: populate star...
                    return star;
                }

                throw new RequestException(404);
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }
    }
}
