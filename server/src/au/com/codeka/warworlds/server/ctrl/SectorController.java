package au.com.codeka.warworlds.server.ctrl;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import au.com.codeka.common.Pair;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.Colony;
import au.com.codeka.warworlds.server.model.Fleet;
import au.com.codeka.warworlds.server.model.FleetUpgrade;
import au.com.codeka.warworlds.server.model.Sector;
import au.com.codeka.warworlds.server.model.Star;

public class SectorController {
    private DataBase db;

    public SectorController() {
        db = new DataBase();
    }
    public SectorController(Transaction t) {
        db = new DataBase(t);
    }

    public List<Sector> getSectors(List<Pair<Long, Long>> coords, boolean generate) throws RequestException {
        List<Sector> sectors;
        try {
            sectors = db.getSectors(coords);
        } catch(Exception e) {
            throw new RequestException(e);
        }
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
        try {
            List<Sector> sectors = db.getSectors(new int[] {sectorId});
            if (sectors.size() > 0) {
                populateSectors(sectors);
                return sectors.get(0);
            }
            return null;
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    public void swapStars(Star star1, Star star2) throws RequestException {
        try {
            db.swapStars(star1, star2);
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    private void populateSectors(List<Sector> sectors) throws RequestException {
        try {
            // for the sectors we fetched, we'll also need the stars, colonies & fleets
            int[] ids = new int[sectors.size()];
            for (int i = 0; i < sectors.size(); i++) {
                ids[i] = sectors.get(i).getID();
            }

            for (Star star : db.getStarsForSectors(ids)) {
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

            for (Colony colony : db.getColoniesForSectors(ids)) {
                for (Sector sector : sectors) {
                    if (colony.getSectorID() == sector.getID()) {
                        for (BaseStar baseStar : sector.getStars()) {
                            Star star = (Star) baseStar;
                            if (star.getID() == colony.getStarID()) {
                                star.getColonies().add(colony);
                            }
                        }
                    }
                }
            }

            for (Fleet fleet : db.getFleetsForSectors(ids)) {
                for (Sector sector : sectors) {
                    if (fleet.getSectorID() == sector.getID()) {
                        for (BaseStar baseStar : sector.getStars()) {
                            Star star = (Star) baseStar;
                            if (star.getID() == fleet.getStarID()) {
                                star.getFleets().add(fleet);
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    private class DataBase extends BaseDataBase {
        public DataBase() {
            super();
        }
        public DataBase(Transaction t) {
            super(t);
        }

        public List<Sector> getSectors(List<Pair<Long, Long>> coords) throws Exception {
            String sql = "SELECT id, x, y, distance_to_centre, num_colonies FROM sectors WHERE (1=0";
            for (Pair<Long, Long> coord : coords) {
                sql += " OR (x="+coord.one+" AND y="+coord.two+")";
            }
            sql += ")";
            return getSectors(sql);
        }

        public List<Sector> getSectors(int[] sectorIds) throws Exception {
            String sql = "SELECT id, x, y, distance_to_centre, num_colonies FROM sectors WHERE id IN ";
            sql += buildInClause(sectorIds);
            return getSectors(sql);
        }

        private List<Sector> getSectors(String sql) throws Exception {
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();

                List<Sector> sectors = new ArrayList<Sector>();
                while (rs.next()) {
                    sectors.add(new Sector(rs));
                }
                return sectors;
            }
        }

        public List<Star> getStarsForSectors(int[] sectorIds) throws Exception {
            String sql = "SELECT stars.id, sector_id, name, sectors.x AS sector_x," +
                               " sectors.y AS sector_y, stars.x, stars.y, size, star_type, planets," +
                               " last_simulation, time_emptied" +
                        " FROM stars" +
                        " INNER JOIN sectors ON stars.sector_id = sectors.id" +
                        " WHERE sector_id IN "+buildInClause(sectorIds);
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();

                List<Star> stars = new ArrayList<Star>();
                while (rs.next()) {
                    stars.add(new Star(rs));
                }
                return stars;
            }
        }

        public List<Colony> getColoniesForSectors(int[] sectorIds) throws Exception {
            String sql = "SELECT colonies.*" +
                         " FROM colonies" +
                         " WHERE sector_id IN "+buildInClause(sectorIds);
             try (SqlStmt stmt = prepare(sql)) {
                 ResultSet rs = stmt.select();

                 List<Colony> colonies = new ArrayList<Colony>();
                 while (rs.next()) {
                     colonies.add(new Colony(rs));
                 }
                 return colonies;
             }
        }

        public List<Fleet> getFleetsForSectors(int[] sectorIds) throws Exception {
            HashSet<Integer> starIds = new HashSet<Integer>();
            List<Fleet> fleets = new ArrayList<Fleet>();

            String sql = "SELECT fleets.*" +
                         " FROM fleets" +
                         " WHERE sector_id IN "+buildInClause(sectorIds);
            try (SqlStmt stmt = prepare(sql)) {
                ResultSet rs = stmt.select();

                while (rs.next()) {
                    Fleet fleet = new Fleet(rs);
                    fleets.add(fleet);
                    if (!starIds.contains(fleet.getStarID())) {
                        starIds.add(fleet.getStarID());
                    }
                }
            }

            if (starIds.size() > 0) {
                sql = "SELECT * FROM fleet_upgrades WHERE star_id IN "+buildInClause(starIds);
                try (SqlStmt stmt = prepare(sql)) {
                    ResultSet rs = stmt.select();
    
                    while (rs.next()) {
                        FleetUpgrade fleetUpgrade = FleetUpgrade.create(rs);
    
                        for (Fleet fleet : fleets) {
                            if (fleet.getID() == fleetUpgrade.getFleetID()) {
                                fleet.getUpgrades().add(fleetUpgrade);
                                break;
                            }
                        }
                    }
                }
            }

            return fleets;
        }

        public void swapStars(Star star1, Star star2) throws Exception {
            if (star1.getSectorX() != star2.getSectorX() ||
                star1.getSectorY() != star2.getSectorY()) {

                int sector1ID, sector2ID;
                String sql = "SELECT id FROM sectors WHERE x = ? AND y = ?";
                try (SqlStmt stmt = prepare(sql)) {
                    stmt.setLong(1, star1.getSectorX());
                    stmt.setLong(2, star1.getSectorY());
                    sector1ID = stmt.selectFirstValue(Integer.class);

                    stmt.setLong(1, star2.getSectorX());
                    stmt.setLong(2, star2.getSectorY());
                    sector2ID = stmt.selectFirstValue(Integer.class);
                }

                String[] sqls = {"UPDATE stars SET sector_id = ? WHERE id = ?",
                                 "UPDATE fleets SET sector_id = ? WHERE star_id = ?",
                                 "UPDATE colonies SET sector_id = ? WHERE star_id = ?"};
                for (String s : sqls) {
                    try (SqlStmt stmt = prepare(s)) {
                        stmt.setInt(1, sector2ID);
                        stmt.setInt(2, star1.getID());
                        stmt.update();

                        stmt.setInt(1, sector1ID);
                        stmt.setInt(2, star2.getID());
                        stmt.update();
                    }
                }

                sql = "UPDATE sectors SET num_colonies =" +
                      " (SELECT COUNT(*) FROM colonies WHERE sector_id = sectors.id AND empire_id IS NOT NULL)" +
                     " WHERE sectors.id IN (?, ?)";
                try (SqlStmt stmt = prepare(sql)) {
                    stmt.setInt(1, star1.getSectorID());
                    stmt.setInt(2, star1.getSectorID());
                    stmt.update();
                }
            }

            String sql = "UPDATE stars SET x = ?, y = ? WHERE id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, star1.getOffsetX());
                stmt.setInt(2, star1.getOffsetY());
                stmt.setInt(3, star2.getID());
                stmt.update();

                stmt.setInt(1, star2.getOffsetX());
                stmt.setInt(2, star2.getOffsetY());
                stmt.setInt(3, star1.getID());
                stmt.update();
            }

            sql = "UPDATE fleets SET target_star_id = ? WHERE target_star_id = ?";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, star1.getID());
                stmt.setInt(2, star2.getID());
                stmt.update();

                stmt.setInt(1, star2.getID());
                stmt.setInt(2, star1.getID());
                stmt.update();
            }

            // any fleets that were moving FROM these stars are transported back home...
            sql = "UPDATE fleets SET target_star_id = NULL, state = 1, eta = NULL" +
                 " WHERE star_id = ? AND state = 2";
            try (SqlStmt stmt = prepare(sql)) {
                stmt.setInt(1, star1.getID());
                stmt.update();

                stmt.setInt(1, star2.getID());
                stmt.update();
            }
        }
    }
}
