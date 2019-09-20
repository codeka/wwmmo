package au.com.codeka.warworlds.server.model;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.cron.AbstractCronJob;
import au.com.codeka.warworlds.server.cron.CronJob;
import au.com.codeka.warworlds.server.data.SqlResult;

public class CronJobDetails {
  private long id;
  private Class<? extends AbstractCronJob> cls;
  private String parameters;

  public CronJobDetails(HttpServletRequest request) throws RequestException {
    String sid = request.getParameter("id");
    if (!sid.isEmpty()) {
      id = Long.parseLong(sid);
    }

    cls = loadClass(request.getParameter("class_name"));
    parameters = request.getParameter("params");
  }

  public CronJobDetails(SqlResult result) throws SQLException, RequestException {
    id = result.getLong("job_id");
    cls = loadClass(result.getString("class_name"));
    parameters = result.getString("params");
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

  public String getClassName() {
    return cls.getName();
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
