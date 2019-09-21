package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeMap;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.cron.CronRunnerThread;
import au.com.codeka.warworlds.server.ctrl.CronController;
import au.com.codeka.warworlds.server.model.CronJobDetails;

/**
 * Handles requests for /admin/cron. Shows the list of cron jobs, lets you register new ones, run
 * jobs immediately and show the last status of the jobs.
 */
public class AdminCronHandler extends AdminHandler {
  private static final Log log = new Log("AdminCronHandler");

  @Override
  public void get() throws RequestException {
    if (!isAdmin()) {
      return;
    }

    TreeMap<String, Object> data = new TreeMap<>();

    ArrayList<JobClassDefinition> classes = new ArrayList<>();
    for (Class<?> jobClass : AbstractCronJob.findAllJobClasses()) {
      classes.add(new JobClassDefinition(jobClass));
    }
    classes.sort((lhs, rhs) -> lhs.annotation.name().compareTo(rhs.annotation.name()));
    data.put("classes", classes);

    data.put("jobs", new CronController().list());

    render("admin/cron/index.html", data);
  }

  @Override
  public void post() throws RequestException {
    switch (getRequest().getParameter("action")) {
      case "edit-cron":
        handleEditCron();
        break;
      case "run-now":
        handleRunNow();
        break;
      default:
        throw new RequestException(400, "Unknown action: " + getRequest().getParameter("action"));
    }

    redirect("/realms/" + getRealm() + "/admin/cron");
  }

  private void handleEditCron() throws RequestException {
    String sid = getRequest().getParameter("id");
    CronJobDetails jobDetails;
    if (!sid.isEmpty()) {
      long id = Long.parseLong(sid);
      jobDetails = new CronController().get(id);
      if (jobDetails == null) {
        throw new RequestException(404, "Invalid job ID: " + id);
      }
    } else {
      jobDetails = new CronJobDetails();
    }
    jobDetails.update(getRequest());
    new CronController().save(jobDetails);

    CronRunnerThread.ping();
  }

  private void handleRunNow() throws RequestException {
    long id = Long.parseLong(getRequest().getParameter("id"));
    CronJobDetails jobDetails = new CronController().get(id);
    if (jobDetails == null) {
      throw new RequestException(404, "No job with that ID found.");
    }

    CronRunnerThread.runNow(jobDetails);
  }

  public static class JobClassDefinition {
    public CronJob annotation;
    public String className;

    public JobClassDefinition(Class<?> cls) {
      this.annotation = cls.getAnnotation(CronJob.class);
      this.className = cls.getName();
    }
  }
}
