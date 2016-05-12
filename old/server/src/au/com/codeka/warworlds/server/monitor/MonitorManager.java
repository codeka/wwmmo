package au.com.codeka.warworlds.server.monitor;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.server.Session;

/**
 * Monitors are used to monitor requests and responses. They're called at various points during the
 * processing of a request. This class manages the collection of monitors.
 */
public class MonitorManager {
  private final ArrayList<Monitor> monitors = new ArrayList<>();

  public MonitorManager() {
    monitors.add(new EmpireIpAddressMonitor());
  }

  /** Called before the request is processed. */
  public void onBeginRequest(Session session, HttpServletRequest request,
      HttpServletResponse response) {
    for (Monitor monitor : monitors) {
      monitor.onBeginRequest(session, request, response);
    }
  }

  /** Called after the request was processed. */
  public void onEndRequest(Session session, HttpServletRequest request,
      HttpServletResponse response) {
    for (Monitor monitor : monitors) {
      monitor.onEndRequest(session, request, response);
    }
  }
}
