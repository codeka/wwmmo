package au.com.codeka.warworlds.server.cron;

import java.util.Map;
import java.util.TreeMap;

import au.com.codeka.common.Log;

/**
 * A registry of the cron jobs we have created.
 */
public class CronJobRegistry {
  private static final Log log = new Log("CronJobRegistry");
  private static Map<String, Class<? extends CronJob>> sCronJobs;

  static {
    sCronJobs = new TreeMap<>();
    sCronJobs.put("update-ranks", UpdateRanksCronJob.class);
    sCronJobs.put("prune-database", PruneDatabaseCronJob.class);
    sCronJobs.put("find-abandoned-empires", FindAbandonedEmpiresCronJob.class);
    sCronJobs.put("find-alts", FindAltAccountsCronJob.class);
    sCronJobs.put("update-dashboard", UpdateDashboardCronJob.class);
    sCronJobs.put("fix-wormhole-empire-ids", FixWormholeEmpireIdsJob.class);
    sCronJobs.put("update-active-alliances", UpdateActiveAlliancesCronJob.class);
    sCronJobs.put("patreon", PatreonCronJob.class);
  }

  public static CronJob getJob(String name) {
    Class<? extends CronJob> clazz = sCronJobs.get(name);
    if (clazz != null) {
      try {
        return clazz.newInstance();
      } catch (InstantiationException | IllegalAccessException e) {
        log.error("Exception caught creating instance of cron task '" + name + "'", e);
        return null;
      }
    }
    return null;
  }
}
