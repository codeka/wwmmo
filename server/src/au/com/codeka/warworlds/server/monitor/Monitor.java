package au.com.codeka.warworlds.server.monitor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.Session;

/**
 * Abstract base class for monitors. We implement all the methods so individual monitors can just
 * override the ones they're interested in.
 */
public abstract class Monitor {
  /** Called before the request is processed. */
  public void onBeginRequest(Session session, HttpServletRequest request,
      HttpServletResponse response) throws RequestSuspendedException, RequestException {
  }

  /** Called after the request was processed. */
  public void onEndRequest(Session session, HttpServletRequest request,
      HttpServletResponse response, long processTimeMs) {
  }
}
