package au.com.codeka.warworlds.server.monitor;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.Session;

/**
 * A monitor that stores stats about requests. How many there has been, how long they've taken,
 * who is making them, etc.
 */
public class RequestStatMonitor extends Monitor {
  private final Object lock = new Object();

  public static final RequestStatMonitor i = new RequestStatMonitor();

  private RequestStatMonitor() {
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

  public Messages.RequestStatHour getCurrentHour() {
    return buildStats();
  }

  private Messages.RequestStatHour buildStats() {
    // Clone it inside a lock only so that any other threads can keep adding or whatever.
    Messages.RequestStatHour.Builder stats;
    synchronized (lock) {
      if (currentHour == null) {
        return Messages.RequestStatHour.getDefaultInstance();
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

  private Messages.RequestStatHour.Builder ensureCurrentHour() {
    synchronized (lock) {
      Calendar now = Calendar.getInstance();
      int day = now.get(Calendar.YEAR) * 10000 + now.get(Calendar.MONTH) * 100 +
          now.get(Calendar.DAY_OF_MONTH);
      int hour = now.get(Calendar.HOUR_OF_DAY);

      if (currentHour == null || currentHour.getDay() != day || currentHour.getHour() != hour) {
        if (currentHour != null) {
          Messages.RequestStatHour stats = buildStats();
          // TODO: save to the database.

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
