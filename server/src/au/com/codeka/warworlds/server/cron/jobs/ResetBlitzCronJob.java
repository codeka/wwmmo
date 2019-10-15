package au.com.codeka.warworlds.server.cron.jobs;

import java.util.HashSet;

import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.ctrl.GameHistoryController;
import au.com.codeka.warworlds.server.ctrl.NotificationController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
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

    // Should we wait for the star simulation thread to catch up? It could take several minutes
    // and it probably doesn't make *too* much difference to the final result. For now we'll just
    // re-calculate the ranks at whatever they are at this exact point.

    // Re-calculate all of the ranks.
    new UpdateRanksCronJob().run("");

    // Copy the ranks to the history table.
    String sql = "DELETE FROM empire_rank_histories WHERE game_history_id=?";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setLong(1, currentGame.getId());
      stmt.update();
    }

    sql = "INSERT INTO empire_rank_histories " +
        " (game_history_id, empire_id, rank, total_stars, total_colonies, total_ships," +
        "   total_population, total_buildings)" +
        "    SELECT ?, empire_id, rank, total_stars, total_colonies, total_ships, " +
        "        total_population, total_buildings" +
        "    FROM empire_ranks";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setLong(1, currentGame.getId());
      stmt.update();
    }

    // TODO: actually reset the universe.

    // Mark a new game as beginning!
    new GameHistoryController().startNewGame();

    return "Success.";
  }
}
