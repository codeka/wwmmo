package au.com.codeka.warworlds.server.monitor.ratelimit;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.monitor.Monitor;
import au.com.codeka.warworlds.server.monitor.RequestSuspendedException;

/**
 * RequestRateLimiter limits the request rate that certain empires are allowed to make. For now, we
 * manually assign empires to buckets, and all empires in a single bucket get rate limited together.
 */
public class RequestRateLimiter extends Monitor {
  private static final Log log = new Log("RequestRateLimiter");

  // A mapping of empire IDs to the buckets they belong to.
  private Map<Integer, Bucket> buckets;

  public RequestRateLimiter() {
    RateLimitConfig config = RateLimitConfig.load();

    buckets = new HashMap<>();
    for (RateLimitConfig.Bucket bucketConfig : config.getBuckets()) {
      Bucket bucket = new Bucket(bucketConfig);
      for (Integer empireId : bucketConfig.getEmpireIds()) {
        buckets.put(empireId, bucket);
      }
    }
  }

  /** Called before the request is processed. */
  public void onBeginRequest(Session session, HttpServletRequest request,
                             HttpServletResponse response)
      throws RequestSuspendedException, RequestException {
    if (session == null) {
      return;
    }
    Bucket bucket = buckets.get(session.getEmpireID());
    if (bucket == null) {
      // No rate-limit configured, good to go.
      return;
    }

    if (request.getPathInfo().contains("notifications")) {
      // For now, we'll skip rate-limit the notifications request.
      return;
    }

    Continuation cont = ContinuationSupport.getContinuation(request);
    if (!cont.isInitial()) {
      // This request was already limited, it's good to go.
      return;
    }

    long delayMs = bucket.delayRequest(session, request);
    if (delayMs == 0) {
      // No delay needed, request is fine.
      return;
    }

    cont.setTimeout(delayMs);
    cont.suspend();
    throw new RequestSuspendedException();
  }

}
