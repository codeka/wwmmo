package au.com.codeka.warworlds.server.ctrl;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.data.Transaction;
import au.com.codeka.warworlds.server.model.CronJobDetails;

public class CronController {
  private static final Log log = new Log("CronController");
  private DataBase db;

  public CronController() {
    db = new DataBase();
  }

  public CronController(Transaction trans) {
    db = new DataBase(trans);
  }

  /**
   * Parses a schedule string into a list of {@code LocalTime}s that the schedule represents.
   *
   * A schedule is a string of the form "h m" where "h" represents the hour to run on, and "m" the
   * minute. Each value can take on a couple of special values:
   * <ul>
   *   <li>n - A single number causes us to run on exactly that value only. For example, "4 23"
   *   causes the schedule to run at 4:23am only.
   *   <li>* - An asterisk causes the schedule to run on *all* of those values. For example,
   *   "* 17" runs the schedule a 00:17, 01:17, 02:17, and so on: every hour. You cannot use "*"
   *   for the minutes.
   *   <li>n,m,... - A comma-separated list of numbers runs at those values only. For example,
   *   "* 00,15,30,45" causes the schedule to run every 15 minutes.</li>
   *   <li>/n - A slash followed by a number causes the schedule to run at the given interval.
   *   For example, "/2 30" causes the schedule to run at 00:30, 02:30, 04:30 and so on.</li>
   * </ul>
   *
   * @return The schedule, or null if we weren't able to parse the schedule.
   */
  @Nullable
  public static List<LocalTime> parseSchedule(String schedule) {
    String[] hm = schedule.split(" ");
    if (hm.length != 2) {
      // Need "h" and "m", this isn't value,
      return null;
    }

    if (hm[1].contains("*")) {
      // Minutes don't support asterisk.
      return null;
    }

    List<Integer> hours = expandNumber(hm[0], 24);
    if (hours == null) {
      return null;
    }
    List<Integer> minutes = expandNumber(hm[1], 60);
    if (minutes == null) {
      return null;
    }

    List<LocalTime> times = new ArrayList<>();
    for (int hour : hours) {
      for (int minute : minutes) {
        times.add(new LocalTime(hour, minute));
      }
    }
    return times;
  }

  @Nullable
  private static List<Integer> expandNumber(String number, int range) {
    int interval = 0;
    if (number.equals("*")) {
      interval = 1;
    } else if (number.startsWith("/")) {
      interval = Integer.parseInt(number.substring(1));
    }
    if (interval > 0) {
      List<Integer> numbers = new ArrayList<>();
      for (int i = 0; i < range; i += interval) {
        numbers.add(i);
      }
      return numbers;
    }

    String[] values = number.split(",");
    List<Integer> numbers = new ArrayList<>();
    for (int i = 0; i < values.length; i++) {
      numbers.add(Integer.parseInt(values[i]));
    }
    return numbers;
  }

  public void save(CronJobDetails jobDetails) throws RequestException {
    List<LocalTime> times = parseSchedule(jobDetails.getSchedule());
    if (times == null) {
      throw new RequestException(400, "Could not parse schedule.");
    }
    log.info("Got times: %s", times);

    LocalTime nextTime = null;
    DateTime now = DateTime.now();
    for (LocalTime time : times) {
      if (time.isAfter(now.toLocalTime())) {
        nextTime = time;
        break;
      }
    }
    if (nextTime != null) {
      jobDetails.setNextRunTime(nextTime.toDateTimeToday());
    }

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
        sql = "INSERT INTO cron (class_name, params, schedule, last_run_time, next_run_time,"
            + " enabled, last_status) VALUES (?, ?, ?, ?, ?)";
      } else {
        sql = "UPDATE cron SET class_name = ?, params = ?, schedule = ?, last_run_time = ?,"
            + " next_run_time = ?, enabled = ?, last_status = ? "
            + "WHERE job_id = ?";
      }

      try (SqlStmt stmt = prepare(sql)) {
        stmt.setString(1, jobDetails.getClassName());
        stmt.setString(2, jobDetails.getParameters());
        stmt.setString(3, jobDetails.getSchedule());
        if (jobDetails.getLastRunTime() == null) {
          stmt.setDateTime(4, DateTime.now());
        } else {
          stmt.setDateTime(4, jobDetails.getLastRunTime());
        }
        stmt.setDateTime(5, jobDetails.getNextRunTime());
        stmt.setInt(6, jobDetails.getEnabled() ? 1 : 0);
        stmt.setString(7, jobDetails.getLastStatus());
        if (jobDetails.getId() != 0) {
          stmt.setLong(8, jobDetails.getId());
        }

        stmt.update();
      }
    }

    List<CronJobDetails> list() throws Exception {
      String sql = "SELECT * FROM cron ORDER BY job_id";
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
