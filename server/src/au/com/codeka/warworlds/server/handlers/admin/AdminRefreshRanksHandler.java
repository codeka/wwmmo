package au.com.codeka.warworlds.server.handlers.admin;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.cron.jobs.UpdateRanksCronJob;

/**
 * Runs the UpdateRanksCronJob immediately. TODO: have a generic interface for us to run cron jobs
 * on demand.
 */
public class AdminRefreshRanksHandler extends AdminHandler {
  @Override
  protected void post() throws RequestException {
    if (!isAdmin()) {
      return;
    }

    try {
      UpdateRanksCronJob cron = new UpdateRanksCronJob();
      cron.run("");
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }
}
