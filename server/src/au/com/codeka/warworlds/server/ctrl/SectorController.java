package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Pair;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Sector;

public class SectorController {

    public List<Sector> getSectors(List<Pair<Long, Long>> coords, boolean generate) throws RequestException {
        List<Sector> sectors = DataBase.getSectors(coords);

        if (generate) {
            // if we're supposed to generate new sectors, find any that we still couldn't find
            List<Pair<Long, Long>> missing = new ArrayList<Pair<Long, Long>>();
            for (Pair<Long, Long> coord : coords) {
                boolean isMissing = true;
                for (Sector sector : sectors) {
                    if (sector.getX() == coord.one && sector.getY() == coord.two) {
                        isMissing = false;
                    }
                }
                if (isMissing) {
                    missing.add(coord);
                }
            }

            if (missing.size() > 0) {
                SectorGenerator generator = new SectorGenerator();
                for (Pair<Long, Long> coord : missing) {
                    sectors.add(generator.generate(coord.one, coord.two));
                }
            }
        }

        return sectors;
    }

    private static class DataBase {
        public static List<Sector> getSectors(List<Pair<Long, Long>> coords) throws RequestException {
            String sql = "SELECT id, x, y, distance_to_centre, num_colonies FROM sectors WHERE (1=0";
            for (Pair<Long, Long> coord : coords) {
                sql += " OR (x="+coord.one+" AND y="+coord.two+")";
            }
            sql += ")";

            try (SqlStmt stmt = DB.prepare(sql)) {
                ResultSet rs = stmt.select();
                
                List<Sector> sectors = new ArrayList<Sector>();
                while (rs.next()) {
                    sectors.add(new Sector(rs));
                }
                return sectors;
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }
    }
}
