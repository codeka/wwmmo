package au.com.codeka.warworlds.server.monitor;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.monitor.ratelimit.RequestRateLimiter;

/**
 * Monitors are used to monitor requests and responses. They're called at various points during the
 * processing of a request. This class manages the collection of monitors.
 */
public class MonitorManager {
  private static final Log log = new Log("MonitorManager");
  private final ArrayList<Monitor> monitors = new ArrayList<>();

  public MonitorManager() {
    monitors.add(new EmpireIpAddressMonitor());
    monitors.add(RequestStatMonitor.i);
    monitors.add(new RequestRateLimiter());
  }

  /** Called before the request is processed. */
  public void onBeginRequest(Session session, HttpServletRequest request,
      HttpServletResponse response) throws RequestSuspendedException, RequestException {
    for (Monitor monitor : monitors) {
      try {
        monitor.onBeginRequest(session, request, response);
      } catch (RuntimeException e) {
        log.error("Unhandled error processing onBeginRequest.", e);
      }
    }
  }

  /** Called after the request was processed. */
  public void onEndRequest(Session session, HttpServletRequest request,
      HttpServletResponse response, long processTimeMs) {
    for (Monitor monitor : monitors) {
      try {
        monitor.onEndRequest(session, request, response, processTimeMs);
      } catch (RuntimeException e) {
        log.error("Unhandled error processing onEndRequest.", e);
      }
    }
  }
}
