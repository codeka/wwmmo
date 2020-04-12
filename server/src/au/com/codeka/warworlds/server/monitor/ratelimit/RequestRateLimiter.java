package au.com.codeka.warworlds.server.monitor.ratelimit;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
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

  // If non-null, this is the config we'll use to create buckets for empires that don't already
  // have a bucket configured.
  @Nullable
  private RateLimitConfig.Bucket defBucketConfig;

  public RequestRateLimiter() {
    RateLimitConfig config = RateLimitConfig.load();

    buckets = new HashMap<>();
    for (RateLimitConfig.Bucket bucketConfig : config.getBuckets()) {
      Bucket bucket = new Bucket(bucketConfig);
      for (Integer empireId : bucketConfig.getEmpireIds()) {
        buckets.put(empireId, bucket);
      }
    }
    defBucketConfig = config.getDefaultBucket();
  }

  /** Called before the request is processed. */
  public void onBeginRequest(Session session, HttpServletRequest request,
                             HttpServletResponse response)
      throws RequestSuspendedException, RequestException {
    if (session == null) {
      return;
    }
    if (session.isAdmin()) {
      // Don't rate-limit the admin user.
      return;
    }
    Bucket bucket = buckets.get(session.getEmpireID());
    if (bucket == null) {
      if (defBucketConfig == null) {
        // No rate-limit configured, good to go.
        return;
      }

      log.info("Creating new bucket for empire %d", session.getEmpireID());
      bucket = new Bucket(defBucketConfig);
      buckets.put(session.getEmpireID(), bucket);
    }

    String path = request.getPathInfo();
    if (path.contains("notifications") || path.contains("login") || path.contains("hello")) {
      // For now, we'll skip rate-limiting the notifications request, login and hello.
      return;
    }

    if (request.getMethod().equalsIgnoreCase("get")) {
      // We'll also skip rate-limit GET requests.
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
