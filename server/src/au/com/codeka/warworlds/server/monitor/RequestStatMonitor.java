package au.com.codeka.warworlds.server.monitor;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.Session;

/**
 * A monitor that stores stats about requests. How many there has been, how long they've taken,
 * who is making them, etc.
 */
public class RequestStatMonitor extends Monitor {
  private static final Log log = new Log("RequestStatMonitor");
  private final Object lock = new Object();

  public static final RequestStatMonitor i = new RequestStatMonitor();

  private RequestStatMonitor() {
    loadCurrentHour();
  }

  private Messages.RequestStatHour.Builder currentHour;

  /** Called after the request was processed. */
  @Override
  public void onEndRequest(
      Session session, HttpServletRequest request, HttpServletResponse response,
      long processTimeMs) {

    // Ignore admin sessions, or sessions where we don't know the empire ID.
    if (session == null || session.isAdmin() || session.getEmpireID() == 0) {
      return;
    }

    // Ignore requests that were rate-limited, as they don't really impact performance much.
    if (response.getStatus() == 429) {
      return;
    }

    // If this request is being suspended, then ignore it (we'll pick up the replay)
    Continuation cont = ContinuationSupport.getContinuation(request);
    if (cont.isSuspended()) {
      return;
    }

    synchronized (lock) {
      Messages.RequestStatHour.Builder stat = ensureCurrentHour();
      stat.addRequest(Messages.RequestStatSingle.newBuilder()
          .setTime(System.currentTimeMillis())
          .setEmpireId(session.getEmpireID())
          .setIpAddress(request.getRemoteAddr())
          .setMethod(request.getMethod())
          .setPath(request.getPathInfo())
          .setResponseCode(response.getStatus())
          .setProcessingTimeMs(processTimeMs));
      stat.setTotalRequests(stat.getTotalRequests() + 1);
    }
  }

  /**
   * Called when we are shut down. We'll save our current hour's worth of stats so that we can
   * start back up where we left off after restarting.
   */
  public void cleanup() {
    saveCurrentHour();
  }

  public Messages.RequestStatHour getCurrentHour() {
    return buildStats();
  }

  /**
   * Returns an {@link ArrayList} of the last <c>num</c> hours of stats, including the current
   * hour.
   */
  public ArrayList<Messages.RequestStatHour> getLastHours(int num) {
    ArrayList<Messages.RequestStatHour> stats = new ArrayList<>();
    stats.add(getCurrentHour());

    File baseDir = new File(Configuration.i.getRequestStatsDirectory());
    DateTime dt = DateTime.now().minusHours(1);
    while (num >= 1) {
      File file = new File(
          baseDir,
          String.format(
              Locale.ENGLISH, "%04d/%02d/%04d-%02d-%02d-%02d.pb", dt.getYear(), dt.getMonthOfYear(),
              dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(), dt.getHourOfDay()));
      if (!file.exists()) {
        int day = dt.getYear() * 10000 + dt.getMonthOfYear() * 100 + dt.getDayOfMonth();
        int hour = dt.getHourOfDay();

        stats.add(Messages.RequestStatHour.newBuilder()
            .setDay(day)
            .setHour(hour)
            .build());
      } else {
        try {
          FileInputStream ins = new FileInputStream(file);
          stats.add(Messages.RequestStatHour.parseFrom(ins));
        } catch (IOException e) {
          log.warning("Error parsing proto from file: ", e);
        }
      }

      dt = dt.minusHours(1);
      num--;
    }

    return stats;
  }

  private Messages.RequestStatHour buildStats() {
    // Clone it inside a lock only so that any other threads can keep adding or whatever.
    Messages.RequestStatHour.Builder stats;
    synchronized (lock) {
      if (currentHour == null) {
        DateTime now = DateTime.now();
        int day = now.getYear() * 10000 + now.getMonthOfYear() * 100 + now.getDayOfMonth();
        int hour = now.getHourOfDay();

        return Messages.RequestStatHour.newBuilder().setDay(day).setHour(hour).build();
      }
      stats = currentHour.clone();
    }

    double processingTimeMs = 0.0;
    Map<Integer, EmpireHourInfo> empires = new HashMap<>();
    for (int i = 0; i < stats.getRequestCount(); i++) {
      Messages.RequestStatSingle request = stats.getRequest(i);

      processingTimeMs += request.getProcessingTimeMs();

      EmpireHourInfo empireHourInfo = empires.get(request.getEmpireId());
      if (empireHourInfo == null) {
        empireHourInfo = new EmpireHourInfo(request.getEmpireId());
        empires.put(request.getEmpireId(), empireHourInfo);
      }

      empireHourInfo.processingTimeMs += request.getProcessingTimeMs();
      empireHourInfo.totalRequests ++;

      EmpireHourResponseCodeInfo empireHourResponseCodeInfo =
          empireHourInfo.responseCodes.get(request.getResponseCode());
      if (empireHourResponseCodeInfo == null) {
        empireHourResponseCodeInfo = new EmpireHourResponseCodeInfo(request.getResponseCode());
        empireHourInfo.responseCodes.put(request.getResponseCode(), empireHourResponseCodeInfo);
      }
      empireHourResponseCodeInfo.processingTimeMs += request.getProcessingTimeMs();
      empireHourResponseCodeInfo.totalRequests ++;
    }

    stats.setTotalRequests(stats.getRequestCount());
    stats.setAvgProcessingTimeMs((long)(processingTimeMs / stats.getRequestCount()));
    for (EmpireHourInfo empireInfo : empires.values()) {
      Messages.RequestStatEmpireHour.Builder empireHourBuilder =
          Messages.RequestStatEmpireHour.newBuilder()
              .setEmpireId(empireInfo.empireId)
              .setAvgProcessingTimeMs(
                  (long)(empireInfo.processingTimeMs / empireInfo.totalRequests))
              .setTotalRequests(empireInfo.totalRequests);

      for (EmpireHourResponseCodeInfo responseCodeInfo : empireInfo.responseCodes.values()) {
        empireHourBuilder.addResponseCodes(
            Messages.RequestStatEmpireHour.ResponseCodeStat.newBuilder()
                .setResponseCode(responseCodeInfo.statusCode)
                .setAvgProcessingTimeMs(
                    (long)(responseCodeInfo.processingTimeMs / responseCodeInfo.totalRequests))
                .setTotalRequests(responseCodeInfo.totalRequests)
                .build());
      }
      stats.addEmpire(empireHourBuilder.build());
    }

    return stats.build();
  }

  private void saveCurrentHour() {
    Messages.RequestStatHour stats = buildStats();

    File baseDir = new File(Configuration.i.getRequestStatsDirectory());

    int year = stats.getDay() / 10000;
    int month = (stats.getDay() - (year * 10000)) / 100;
    int day = stats.getDay() - (year * 10000) - (month * 100);
    int hour = stats.getHour();
    File file = new File(
        baseDir,
        String.format(
            Locale.ENGLISH, "%04d/%02d/%04d-%02d-%02d-%02d.pb", year, month, year, month,
            day, hour));
    File parent = file.getParentFile();
    if (!parent.exists()) {
      if (!parent.mkdirs()) {
        log.warning("Couldn't create parent directory. This is probably going to fail.");
      }
    }

    try {
      FileOutputStream outs = new FileOutputStream(file);
      outs.write(stats.toByteArray());
      outs.close();
    } catch (IOException e) {
      log.error("Error writing stats.", e);
    }
  }

  /** Attempt to load the current hour from disk, if there is one. */
  private void loadCurrentHour() {
    File baseDir = new File(Configuration.i.getRequestStatsDirectory());

    DateTime dt = DateTime.now();
    File file = new File(
        baseDir,
        String.format(
            Locale.ENGLISH, "%04d/%02d/%04d-%02d-%02d-%02d.pb", dt.getYear(), dt.getMonthOfYear(),
            dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(), dt.getHourOfDay()));
    log.info("Attempting to load stats from: %s", file.getAbsolutePath());
    if (file.exists()) {
      try {
        FileInputStream ins = new FileInputStream(file);
        currentHour = Messages.RequestStatHour.parseFrom(ins).toBuilder();
      } catch (IOException e) {
        log.warning("Error parsing proto from file: ", e);
      }
    }
  }

  private Messages.RequestStatHour.Builder ensureCurrentHour() {
    synchronized (lock) {
      DateTime now = DateTime.now();
      int day = now.getYear() * 10000 + now.getMonthOfYear() * 100 + now.getDayOfMonth();
      int hour = now.getHourOfDay();

      if (currentHour == null || currentHour.getDay() != day || currentHour.getHour() != hour) {
        if (currentHour != null) {
          saveCurrentHour();
        }

        currentHour = Messages.RequestStatHour.newBuilder()
            .setDay(day)
            .setHour(hour);
      }

      return currentHour;
    }
  }

  private static class EmpireHourInfo {
    int empireId;
    Map<Integer, EmpireHourResponseCodeInfo> responseCodes;
    double processingTimeMs;
    int totalRequests;

    EmpireHourInfo(int empireId) {
      this.empireId = empireId;
      responseCodes = new HashMap<>();
    }
  }

  private static class EmpireHourResponseCodeInfo {
    int statusCode;
    int totalRequests;
    double processingTimeMs;

    public EmpireHourResponseCodeInfo(int statusCode) {
      this.statusCode = statusCode;
    }
  }
}
