package au.com.codeka.warworlds.server.cron;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A registry of the cron jobs we have created. */
public class CronJobRegistry {
    private static final Logger log = LoggerFactory.getLogger(CronJobRegistry.class);
    private static Map<String, Class<? extends CronJob>> sCronJobs;

    static {
        sCronJobs = new TreeMap<String, Class<? extends CronJob>>();
        sCronJobs.put("update-ranks", UpdateRanksCronJob.class);
        sCronJobs.put("simulate-stars", SimulateAllStarsCronJob.class);
        sCronJobs.put("purge-combat-reports", PurgeCombatReportsCronJob.class);
        sCronJobs.put("purge-sessions", PurgeSessionsCronJob.class);
        sCronJobs.put("find-abandoned-empires", FindAbandonedEmpiresCronJob.class);
        sCronJobs.put("find-alts", FindAltAccountsCronJob.class);
        sCronJobs.put("update-dashboard", UpdateDashboardCronJob.class);
    }

    public static CronJob getJob(String name) {
        Class<? extends CronJob> clazz = sCronJobs.get(name);
        if (clazz != null) {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("Exception caught creating instance of cron task '"+name+"'", e);
                return null;
            }
        }
        return null;
    }
}
