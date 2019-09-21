package au.com.codeka.warworlds.server.model;

import org.joda.time.DateTime;

import java.sql.SQLException;

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

  public CronJobDetails(SqlResult result) throws SQLException, RequestException {
    id = result.getLong("job_id");
    cls = loadClass(result.getString("class_name"));
    parameters = result.getString("params");
    schedule = result.getString("schedule");
    lastRunTime = result.getDateTime("last_run_time");
    nextRunTime = result.getDateTime("next_run_time");
    lastStatus = result.getString("last_status");
    enabled = result.getInt("enabled") != 0;
  }

  public void update(HttpServletRequest request) throws RequestException {
    cls = loadClass(request.getParameter("class_name"));
    parameters = request.getParameter("params");
    schedule = request.getParameter("schedule");
    enabled = request.getParameterValues("enabled") != null;
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

  public void setLastStatus(String status) {
    lastStatus = status;
  }

  @Nullable
  public DateTime getLastRunTime() {
    return lastRunTime;
  }

  public void setLastRunTime(@Nullable DateTime time) {
    lastRunTime = time;
  }

  public DateTime getNextRunTime() {
    return nextRunTime;
  }

  public void setNextRunTime(DateTime time) {
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
