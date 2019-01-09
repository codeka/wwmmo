package au.com.codeka.warworlds.server.ctrl;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;

public class BattleRankController {
  private DataBase db;

  public BattleRankController() {
    db = new DataBase();
  }
  public BattleRankController(Transaction trans) {
    db = new DataBase(trans);
  }

  /**
   * When an empire destroys another empire's fleet, call this to record that.
   *
   * @param empireID The empire that destroyed a fleet.
   * @param numShips The number of ships in the fleet that were destroyed.
   */
  public void recordFleetDestroyed(long empireID, double numShips) throws RequestException {
    try {
      db.recordFleetDestroyed(empireID, numShips);
    } catch(Exception e) {
      throw new RequestException(e);
    }
  }

  /**
   * When an empire destroys another empire's colony, call this to record that.
   *
   * @param empireID The empire that destroyed a fleet.
   * @param population The population of the colony you just destroyed.
   */
  public void recordColonyDestroyed(long empireID, double population) throws RequestException {
    try {
      db.recordColonyDestroyed(empireID, population);
    } catch(Exception e) {
      throw new RequestException(e);
    }
  }

  private static class DataBase extends BaseDataBase {
    /**
     * We keep a map of the most recent battle ranks, to avoid having to query the database every
     * time.
     */
    private static Map<Long, BattleRankInfo> battleRanks = new HashMap<>();

    public DataBase() {
      super();
    }
    public DataBase(Transaction trans) {
      super(trans);
    }

    private static int dateTimeToDay(DateTime dt) {
      return dt.year().get() * 10000 + dt.monthOfYear().get() * 100 + dt.dayOfMonth().get();
    }

    public void recordFleetDestroyed(long empireID, double numShips) throws Exception {
      int day = dateTimeToDay(DateTime.now());
      BattleRankInfo battleRankInfo = getBattleRank(empireID, day);
      battleRankInfo.numShips += numShips;
      updateBattleRank(battleRankInfo);
    }

    public void recordColonyDestroyed(long empireID, double population) throws Exception {
      int day = dateTimeToDay(DateTime.now());
      BattleRankInfo battleRankInfo = getBattleRank(empireID, day);
      battleRankInfo.numColonies ++;
      battleRankInfo.numPopulation += population;
      updateBattleRank(battleRankInfo);
    }

    private void updateBattleRank(BattleRankInfo battleRank) throws Exception {
      String sql = "DELETE FROM empire_battle_ranks WHERE empire_id = ? AND day = ?";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setLong(1, battleRank.empireID);
        stmt.setInt(2, battleRank.day);
        stmt.update();
      }

      sql = "INSERT INTO empire_battle_ranks (" +
          "  empire_id, day, ships_destroyed, population_destroyed, colonies_destroyed" +
          ") VALUES (?, ?, ?, ?, ?)";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setLong(1, battleRank.empireID);
        stmt.setInt(2, battleRank.day);
        stmt.setLong(3, battleRank.numShips);
        stmt.setLong(4, battleRank.numPopulation);
        stmt.setLong(5, battleRank.numColonies);
        stmt.update();
      }
    }

    private BattleRankInfo getBattleRank(long empireID, int day) throws Exception {
      BattleRankInfo battleRank = battleRanks.get(empireID);
      if (battleRank != null) {
        // Only if it's for the same day will we just return this one
        if (battleRank.day == day) {
          return battleRank;
        }

        // If it's for a different day, they actually we can just return a brand new one, it's
        // guaranteed that the database doesn't have anything yet for today.
        return new BattleRankInfo(empireID, day);
      }

      String sql = "SELECT" +
          "  empire_id, day, ships_destroyed, population_destroyed, colonies_destroyed " +
          "FROM empire_battle_ranks " +
          "WHERE empire_id = ?" +
          "  AND day = ?";
      try (SqlStmt stmt = prepare(sql)) {
        stmt.setLong(1, empireID);
        stmt.setInt(2, day);
        SqlResult res = stmt.select();
        if (res.next()) {
          battleRank = new BattleRankInfo(empireID, day);
          battleRank.numShips = res.getLong("ships_destroyed");
          battleRank.numPopulation = res.getLong("population_destroyed");
          battleRank.numColonies = res.getLong("colonies_destroyed");
          return battleRank;
        }
      }

      return new BattleRankInfo(empireID, day);
    }
  }

  private static class BattleRankInfo {
    public long empireID;
    public int day;
    public long numShips;
    public long numPopulation;
    public long numColonies;

    public BattleRankInfo(long empireID, int day) {
      this.empireID = empireID;
      this.day = day;
    }
  }
}
