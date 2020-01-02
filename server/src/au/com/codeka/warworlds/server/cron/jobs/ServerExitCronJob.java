package au.com.codeka.warworlds.server.cron.jobs;

import au.com.codeka.warworlds.server.Runner;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;

@CronJob(
    name = "Shutdown server",
    desc = "Shuts down the server. Parameter is the status code to return (anything non-zero will cause us to be auto-restarted).")
public class ServerExitCronJob extends AbstractCronJob {
  @Override
  public String run(String extra) throws Exception {
    int exitCode = 0;
    try {
      exitCode = Integer.parseInt(extra);
    } catch (NumberFormatException e) {
      return "Invalid 'exit' parameter: " + extra;
    }

    // Don't run this in the request thread, we'll kick off another thread to actually shut down.
    new StopRunnerThread(exitCode).start();

    return "Success";
  }

  private static final class StopRunnerThread extends Thread {
    private final int exitCode;

    StopRunnerThread(int exitCode) {
      this.exitCode = exitCode;
    }

    @Override
    public void run() {
      Runner.stop(exitCode);
    }
  }
}
