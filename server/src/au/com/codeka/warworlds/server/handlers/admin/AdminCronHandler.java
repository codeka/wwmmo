package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.TreeMap;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;

/**
 * Handles requests for /admin/cron. Shows the list of cron jobs, lets you register new ones, run
 * jobs immediately and show the last status of the jobs.
 */
public class AdminCronHandler extends AdminHandler {
  @Override
  public void get() throws RequestException {
    if (!isAdmin()) {
      return;
    }

    TreeMap<String, Object> data = new TreeMap<>();

    ArrayList<JobDetails> jobs = new ArrayList<>();
    for (Class<?> jobClass : AbstractCronJob.findAllJobs()) {
      jobs.add(new JobDetails(jobClass));
    }
    data.put("jobs", jobs);

    render("admin/cron/index.html", data);
  }

  public static class JobDetails {
    public CronJob annotation;
    public String className;

    public JobDetails(Class<?> cls) {
      this.annotation = cls.getAnnotation(CronJob.class);
      this.className = cls.getName();
    }
  }
}
