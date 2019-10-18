package au.com.codeka.warworlds.server.cron.jobs;

import java.sql.SQLException;
import java.util.HashSet;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.StarSimulatorThreadManager;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.ctrl.GameHistoryController;
import au.com.codeka.warworlds.server.ctrl.NotificationController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.GameHistory;

/**
 * This is a cron job that resets the entire universe and starts up a new one.
 */
@CronJob(
    name = "Reset Blitz",
    desc = "Resets the universe and starts up a new server mode for the month.")
public class ResetBlitzCronJob extends AbstractCronJob {
  @Override
  public String run(String extra) throws Exception {
    GameHistory currentGame = new GameHistoryController().markResetting();

    // Send a notification to everyone online that the game is being reset. They'll immediately
    // jump back to the log in screen. That way we avoid the weirdness of trying to do things with
    // a broken game state.
    new NotificationController().sendNotificationToAllOnline("blitz_reset", "", new HashSet<>());

    // Stop the star simulator thread, so no stars get simulated while we're resetting.
    StarSimulatorThreadManager.i.stop();

    // Re-calculate all of the ranks.
    new UpdateRanksCronJob().run("");

    try (Transaction t = DB.beginTransaction()) {
      // Copy the ranks to the history table.
      String sql = "DELETE FROM empire_rank_histories WHERE game_history_id=?";
      try (SqlStmt stmt = t.prepare(sql)) {
        stmt.setLong(1, currentGame.getId());
        stmt.update();
      }

      sql = "INSERT INTO empire_rank_histories " +
          " (game_history_id, empire_id, rank, total_stars, total_colonies, total_ships," +
          "   total_population, total_buildings)" +
          "    SELECT ?, empire_id, rank, total_stars, total_colonies, total_ships, " +
          "        total_population, total_buildings" +
          "    FROM empire_ranks";
      try (SqlStmt stmt = t.prepare(sql)) {
        stmt.setLong(1, currentGame.getId());
        stmt.update();
      }

      final String[] queries = {
          "DELETE FROM abandoned_stars",
          "DELETE FROM scout_reports",
          "DELETE FROM combat_reports",
          "DELETE FROM situation_reports",
          "DELETE FROM build_requests",
          "UPDATE empires SET home_star_id = NULL",
          "DELETE FROM buildings",
          "DELETE FROM colonies",
          "DELETE FROM empire_ranks",
          "DELETE FROM empire_presences",
          "DELETE FROM fleet_upgrades",
          "DELETE FROM fleets",
          "DELETE FROM star_renames",
          "DELETE FROM stars",
          "DELETE FROM sectors",
          "UPDATE empires SET reset_reason = 'blitz'"
      };
      for (String query : queries) {
        try (SqlStmt stmt = t.prepare(query)) {
          stmt.update();
        } catch (SQLException e) {
          throw new RequestException(e, query);
        }
      }

      t.commit();
    }

    // Mark a new game as beginning!
    new GameHistoryController().startNewGame();

    // Restart the star simulator thread
    StarSimulatorThreadManager.i.start();

    // Re-calculate all of the ranks once more.
    new UpdateRanksCronJob().run("");

    return "Success.";
  }
}
