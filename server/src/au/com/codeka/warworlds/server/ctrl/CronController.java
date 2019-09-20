package au.com.codeka.warworlds.server.ctrl;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.CronJobDetails;

public class CronController {
  private DataBase db;

  public CronController() {
    db = new DataBase();
  }

  public CronController(Transaction trans) {
    db = new DataBase(trans);
  }

  public void save(CronJobDetails jobDetails) throws RequestException {
    try {
      db.save(jobDetails);
    } catch(Exception e) {
      throw new RequestException(e);
    }
  }

  public List<CronJobDetails> list() throws RequestException {
    try {
      return db.list();
    } catch(Exception e) {
      throw new RequestException(e);
    }
  }

  @Nullable
  public CronJobDetails get(long id) throws RequestException {
    List<CronJobDetails> jobs = list();
    for (CronJobDetails job : jobs) {
      if (job.getId() == id) {
        return job;
      }
    }
    return null;
  }

  private static class DataBase extends BaseDataBase {
    DataBase() {
      super();
    }

    DataBase(Transaction trans) {
      super(trans);
    }

    void save(CronJobDetails jobDetails) throws Exception {
      String sql;
      if (jobDetails.getId() == 0) {
        sql = "INSERT INTO cron (class_name, params, schedule, last_run_time, next_run_time)"
            + " VALUES (?, ?, ?, ?, ?)";
      } else {
        sql = "UPDATE cron SET class_name = ?, params = ?, schedule = ?, last_run_time = ?,"
            + " next_run_time = ? "
            + "WHERE job_id = ?";
      }

      try (SqlStmt stmt = prepare(sql)) {
        stmt.setString(1, jobDetails.getClassName());
        stmt.setString(2, jobDetails.getParameters());
        stmt.setString(3, "" /* TODO: schedule */);
        stmt.setDateTime(4, new DateTime() /* TODO: last_run_time */);
        stmt.setDateTime(5, new DateTime() /* TODO: next_run_time */);
        if (jobDetails.getId() != 0) {
          stmt.setLong(6, jobDetails.getId());
        }

        stmt.update();
      }
    }

    List<CronJobDetails> list() throws Exception {
      String sql = "SELECT * FROM cron";
      try (SqlStmt stmt = prepare(sql)) {
        SqlResult result = stmt.select();

        ArrayList<CronJobDetails> jobs = new ArrayList<>();
        while (result.next()) {
          jobs.add(new CronJobDetails(result));
        }

        return jobs;
      }
    }
  }
}
