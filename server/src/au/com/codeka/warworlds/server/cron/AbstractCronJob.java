package au.com.codeka.warworlds.server.cron;

import org.reflections.Reflections;

import java.util.Set;

/**
 * This is the base class for all cron jobs.
 */
public abstract class AbstractCronJob {
  public abstract void run(String extra) throws Exception;

  public static Set<Class<?>> findAllJobClasses() {
    return new Reflections("au.com.codeka.warworlds.server")
        .getTypesAnnotatedWith(CronJob.class);
  }
}
