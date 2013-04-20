package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import au.com.codeka.common.Pair;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

public class SectorController {

    public List<Sector> getSectors(List<Pair<Long, Long>> coords, boolean generate) throws RequestException {
        List<Sector> sectors = DataBase.getSectors(coords);
        if (sectors.size() > 0) {
            populateSectors(sectors);
        }

        if (generate && sectors.size() < coords.size()) {
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

    public Sector getSector(int sectorId) throws RequestException {
        List<Sector> sectors = DataBase.getSectors(new int[] {sectorId});
        if (sectors.size() > 0) {
            populateSectors(sectors);
            return sectors.get(0);
        }
        return null;
    }

    private void populateSectors(List<Sector> sectors) throws RequestException {
        // for the sectors we fetched, we'll also need the stars, colonies & fleets
        int[] ids = new int[sectors.size()];
        for (int i = 0; i < sectors.size(); i++) {
            ids[i] = sectors.get(i).getID();
        }

        List<Star> stars = DataBase.getStarsForSectors(ids);
        for (Star star : stars) {
            // add the star to the correct sector
            for (Sector sector : sectors) {
                if (star.getSectorID() == sector.getID()) {
                    sector.getStars().add(star);
                    break;
                }
            }

            star.setColonies(new ArrayList<BaseColony>());
            star.setFleets(new ArrayList<BaseFleet>());
        }

        // todo: colonies
        // todo: fleets
    }

    private static class DataBase {
        public static List<Sector> getSectors(List<Pair<Long, Long>> coords) throws RequestException {
            String sql = "SELECT id, x, y, distance_to_centre, num_colonies FROM sectors WHERE (1=0";
            for (Pair<Long, Long> coord : coords) {
                sql += " OR (x="+coord.one+" AND y="+coord.two+")";
            }
            sql += ")";
            return getSectors(sql);
        }

        public static List<Sector> getSectors(int[] sectorIds) throws RequestException {
            String sql = "SELECT id, x, y, distance_to_centre, num_colonies FROM sectors WHERE id IN ";
            sql += buildInClause(sectorIds);
            return getSectors(sql);
        }

        private static List<Sector> getSectors(String sql) throws RequestException {
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

        public static List<Star> getStarsForSectors(int[] sectorIds) throws RequestException {
            String sql = "SELECT stars.id, sector_id, name, sectors.x AS sector_x," +
                               " sectors.y AS sector_y, stars.x, stars.y, size, star_type, planets," +
                               " last_simulation, time_emptied" +
                        " FROM stars" +
                        " INNER JOIN sectors ON stars.sector_id = sectors.id" +
                        " WHERE sector_id IN "+buildInClause(sectorIds);

            try (SqlStmt stmt = DB.prepare(sql)) {
                ResultSet rs = stmt.select();

                List<Star> stars = new ArrayList<Star>();
                while (rs.next()) {
                    stars.add(new Star(rs));
                }
                return stars;
            } catch(Exception e) {
                throw new RequestException(500, e);
            }
        }

        private static String buildInClause(int[] ids) {
            StringBuffer sb = new StringBuffer();
            sb.append("(");
            for (int i = 0; i < ids.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(ids[i]);
            }
            sb.append(")");
            return sb.toString();
        }
    }
}
