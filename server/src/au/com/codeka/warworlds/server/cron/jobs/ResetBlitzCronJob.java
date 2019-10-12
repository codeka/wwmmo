package au.com.codeka.warworlds.server.cron.jobs;

import java.util.HashSet;

import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.ctrl.GameHistoryController;
import au.com.codeka.warworlds.server.ctrl.NotificationController;

/**
 * This is a cron job that resets the entire universe and starts up a new one.
 */
@CronJob(
    name = "Reset Blitz",
    desc = "Resets the universe and starts up a new server mode for the month.")
public class ResetBlitzCronJob extends AbstractCronJob {
  @Override
  public String run(String extra) throws Exception {
    new GameHistoryController().markResetting();

    // Send a notification to everyone online that the game is being reset. They'll immediately
    // jump back to the log in screen. That way we avoid the weirdness of trying to do things with
    // a broken game state.
    new NotificationController().sendNotificationToAllOnline("blitz_reset", "", new HashSet<>());

    // simulate doing some work for a bit.
    Thread.sleep(5000);



    // Mark a new game as beginning!
    new GameHistoryController().startNewGame();

    return "Success.";
  }
}
