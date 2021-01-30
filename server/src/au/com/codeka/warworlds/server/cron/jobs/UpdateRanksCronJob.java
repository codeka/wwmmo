package au.com.codeka.warworlds.server.cron.jobs;

import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;

import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.EmpireRank;

/**
 * Updates the ranks of empires.
 */
@CronJob(name = "Update Ranks", desc = "Updates the empire ranks.")
public class UpdateRanksCronJob extends AbstractCronJob {
  @Override
  public String run(String extra) throws Exception {
    TreeMap<Integer, EmpireRank> ranks = new TreeMap<>();

    String sql = "SELECT id AS empire_id FROM empires WHERE state <> 2";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        EmpireRank rank = new EmpireRank(res);
        ranks.put(rank.getEmpireID(), rank);
      }
    }

    sql = "SELECT empire_id, SUM(num_ships) " +
        "FROM fleets WHERE empire_id IS NOT NULL GROUP BY empire_id";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        int empireID = res.getInt(1);
        long totalShips = res.getLong(2);
        if (!ranks.containsKey(empireID)) {
          continue;
        }
        ranks.get(empireID).setTotalShips(totalShips);
      }
    }

    sql = "SELECT empire_id, COUNT(*) " +
        "FROM buildings WHERE empire_id IS NOT NULL GROUP BY empire_id";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        int empireID = res.getInt(1);
        long totalBuildings = res.getLong(2);
        if (!ranks.containsKey(empireID)) {
          continue;
        }
        ranks.get(empireID).setTotalBuildings(totalBuildings);
      }
    }

    sql = "SELECT empire_id, COUNT(*), SUM(population) " +
        "FROM colonies WHERE empire_id IS NOT NULL GROUP BY empire_id";
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      while (res.next()) {
        int empireID = res.getInt(1);
        long totalColonies = res.getLong(2);
        long totalPopulation = res.getLong(3);
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
        long totalStars = res.getLong(2);
        if (!ranks.containsKey(empireID)) {
          continue;
        }
        ranks.get(empireID).setTotalStars(totalStars);
      }
    }

    ArrayList<EmpireRank> sortedRanks = new ArrayList<>(ranks.values());
    sortedRanks.sort((left, right) -> {
      long diff = right.getTotalPopulation() - left.getTotalPopulation();

      if (diff != 0)
        return (int) diff;

      diff = right.getTotalColonies() - left.getTotalColonies();
      if (diff != 0)
        return (int) diff;

      diff = right.getTotalStars() - left.getTotalStars();
      if (diff != 0)
        return (int) diff;

      diff = right.getTotalShips() - left.getTotalShips();
      return (int) diff;
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
          stmt.setLong(3, rank.getTotalStars());
          stmt.setLong(4, rank.getTotalColonies());
          stmt.setLong(5, rank.getTotalBuildings());
          stmt.setLong(6, rank.getTotalShips());
          stmt.setLong(7, rank.getTotalPopulation());
          stmt.update();

          rankValue++;
        }
      }

      t.commit();
    }

    // Also update the alliance's "total_stars" field while we're here.
    sql = " UPDATE alliances" +
        " SET" +
        "   total_stars=sub.total_stars" +
        " FROM (" +
        "   SELECT" +
        "     alliances.id," +
        "     SUM(empire_ranks.total_stars) AS total_stars" +
        "   FROM alliances" +
        "   INNER JOIN empires" +
        "     ON alliances.id = empires.alliance_id" +
        "   INNER JOIN empire_ranks" +
        "     ON empires.id = empire_ranks.empire_id" +
        "   GROUP BY alliances.id" +
        " ) AS sub" +
        " WHERE alliances.id = sub.id";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.update();
    }

    return String.format(Locale.ENGLISH, "%d empire ranks updated.", sortedRanks.size());
  }
}
