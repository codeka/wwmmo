package au.com.codeka.warworlds.server.cron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.EmpireRank;

/**
 * Updates the ranks of empires.
 */
public class UpdateRanksCronJob extends CronJob {
    @Override
    public void run(String extra) throws Exception {
        TreeMap<Integer, EmpireRank> ranks = new TreeMap<Integer, EmpireRank>();

        String sql = "SELECT id AS empire_id FROM empires WHERE state <> 2";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                EmpireRank rank = new EmpireRank(res);
                ranks.put(rank.getEmpireID(), rank);
            }
        }

        sql = "SELECT empire_id, SUM(num_ships) FROM fleets WHERE empire_id IS NOT NULL GROUP BY empire_id";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                int empireID = res.getInt(1);
                BigInteger totalShips = BigInteger.valueOf(res.getLong(2));
                if (!ranks.containsKey(empireID)) {
                    continue;
                }
                ranks.get(empireID).setTotalShips(totalShips);
            }
        }

        sql = "SELECT empire_id, COUNT(*) FROM buildings GROUP BY empire_id";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                int empireID = res.getInt(1);
                BigInteger totalBuildings = BigInteger.valueOf(res.getLong(2));
                if (!ranks.containsKey(empireID)) {
                    continue;
                }
                ranks.get(empireID).setTotalBuildings(totalBuildings);
            }
        }

        sql = "SELECT empire_id, COUNT(*), SUM(population) FROM colonies WHERE empire_id IS NOT NULL GROUP BY empire_id";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                int empireID = res.getInt(1);
                BigInteger totalColonies = BigInteger.valueOf(res.getLong(2));
                BigInteger totalPopulation = BigInteger.valueOf(res.getLong(3));
                if (!ranks.containsKey(empireID)) {
                    continue;
                }
                ranks.get(empireID).setTotalColonies(totalColonies);
                ranks.get(empireID).setTotalPopulation(totalPopulation);
            }
        }

        sql = "SELECT empire_id, COUNT(*) FROM (" +
               " SELECT empire_id, star_id" +
               " FROM stars" +
               " INNER JOIN colonies ON colonies.star_id = stars.id" +
               " WHERE colonies.empire_id IS NOT NULL" +
               " GROUP BY empire_id, star_id" +
              ") AS stars" +
             " GROUP BY empire_id";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                int empireID = res.getInt(1);
                BigInteger totalStars = BigInteger.valueOf(res.getLong(2));
                if (!ranks.containsKey(empireID)) {
                    continue;
                }
                ranks.get(empireID).setTotalStars(totalStars);
            }
        }

        ArrayList<EmpireRank> sortedRanks = new ArrayList<EmpireRank>(ranks.values());
        Collections.sort(sortedRanks, new Comparator<EmpireRank>() {
            @Override
            public int compare(EmpireRank left, EmpireRank right) {
                int diff = right.getTotalPopulation().subtract( left.getTotalPopulation() ).intValue();

                if (diff != 0)
                    return diff;

                diff = right.getTotalColonies().subtract( left.getTotalColonies() ).intValue();
                if (diff != 0)
                    return diff;

                diff = right.getTotalStars().subtract( left.getTotalStars() ).intValue();
                if (diff != 0)
                    return diff;

                diff = right.getTotalShips().subtract( left.getTotalShips() ).intValue();
                return diff;
            }
        });

        try (Transaction t = DB.beginTransaction()) {
            sql = "DELETE FROM empire_ranks WHERE empire_id = ?";
            try (SqlStmt stmt = t.prepare(sql)) {
                for (EmpireRank rank : sortedRanks) {
                    stmt.setInt(1, rank.getEmpireID());
                    stmt.update();
                }
            }

            sql = "INSERT INTO empire_ranks (empire_id, rank, total_stars, total_colonies," +
                                           " total_buildings, total_ships, total_population)" +
                 " VALUES (?, ?, ?, ?, ?, ?, ?);";
            try (SqlStmt stmt = t.prepare(sql)) {
                int rankValue = 1;
                for (EmpireRank rank : sortedRanks) {
                    stmt.setInt(1, rank.getEmpireID());
                    stmt.setInt(2, rankValue);
                    stmt.setLong(3, rank.getTotalStars().longValue());
                    stmt.setLong(4, rank.getTotalColonies().longValue());
                    stmt.setLong(5, rank.getTotalBuildings().longValue());
                    stmt.setLong(6, rank.getTotalShips().longValue());
                    stmt.setLong(7, rank.getTotalPopulation().longValue());
                    stmt.update();

                    rankValue ++;
                }
            }

            t.commit();
        }
    }
}
