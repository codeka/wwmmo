package au.com.codeka.warworlds.server.model;

import org.joda.time.DateTime;

import java.sql.SQLException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.data.SqlResult;

public class CronJobDetails {
  private long id;
  private Class<? extends AbstractCronJob> cls;
  private String parameters;
  private String schedule;
  private DateTime lastRunTime;
  private DateTime nextRunTime;
  private String lastStatus;
  private boolean enabled;
  private boolean runOnce;

  public CronJobDetails() {
    id = 0;
    nextRunTime = new DateTime();
  }

  public CronJobDetails(SqlResult result) throws SQLException, RequestException {
    id = result.getLong("job_id");
    cls = loadClass(result.getString("class_name"));
    parameters = result.getString("params");
    schedule = result.getString("schedule");
    lastRunTime = result.getDateTime("last_run_time");
    nextRunTime = result.getDateTime("next_run_time");
    lastStatus = result.getString("last_status");
    enabled = result.getInt("enabled") != 0;
    runOnce = result.getInt("run_once") != 0;
  }

  public void update(HttpServletRequest request) throws RequestException {
    cls = loadClass(request.getParameter("class_name"));
    parameters = request.getParameter("params");
    schedule = request.getParameter("schedule");
    enabled = request.getParameterValues("enabled") != null;
    runOnce = request.getParameterValues("run_once") != null;
  }

  public long getId() {
    return id;
  }

  public CronJob getAnnotation() {
    return cls.getAnnotation(CronJob.class);
  }

  public AbstractCronJob createInstance() throws InstantiationException, IllegalAccessException {
    return cls.newInstance();
  }

  public String getParameters() {
    return parameters;
  }

  public String getSchedule() {
    return schedule;
  }

  public String getClassName() {
    return cls.getName();
  }

  public boolean getEnabled() {
    return enabled;
  }

  public String getLastStatus() {
    return lastStatus;
  }

  @Nullable
  public DateTime getLastRunTime() {
    return lastRunTime;
  }

  public DateTime getNextRunTime() {
    return nextRunTime;
  }

  public boolean getRunOnce() {
    return runOnce;
  }

  public void setLastStatus(String status) {
    lastStatus = status;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setLastRunTime(@Nullable DateTime time) {
    lastRunTime = time;
  }

  public void setNextRunTime(@Nonnull DateTime time) {
    nextRunTime = time;
  }


  @SuppressWarnings("unchecked")
  private static Class<? extends AbstractCronJob> loadClass(String className)
      throws RequestException {
    try {
      return (Class<? extends AbstractCronJob>) Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new RequestException(e);
    }
  }
}
