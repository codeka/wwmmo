package au.com.codeka.warworlds.server.monitor.ratelimit;

import com.google.api.client.util.Objects;

import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.Session;

/**
 * A Bucket is a single bucket in the "leaky-bucket" style of request rate limiting. Multiple
 * empires can be placed in a single bucket via configuration and they'll all have a shared rate
 * limit.
 */
public class Bucket {
  private static final Log log = new Log("Bucket");
  private final Object lock = new Object();

  private static class Limit {
    double qps;
    int maxSize;
    double size;

    Limit(RateLimitConfig.BucketLimit config) {
      qps = config.getQps();
      maxSize = config.getSize();
      size = maxSize;
    }

    boolean allow(long msSinceLastRequest) {
      size += qps * (msSinceLastRequest / 1000.0);
      if (size > maxSize) {
        size = maxSize;
      }

      // Also decrement, since this is a request
      size --;
      if (size < 0) {
        // By not resetting size to 0 here, if you keep making requests, it takes even longer
        // to restore your counter to non-zero.
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this)
          .add("qps", qps)
          .add("maxSize", maxSize)
          .add("size", size)
          .toString();
    }
  }

  private long lastRequestTime;
  private DateTime currentHour;
  private int numRequestsThisHour;

  @Nullable private final Limit hard;
  @Nullable private final Limit soft;
  private final long delayMs;
  private final int maxRequestsPerHour;

  // Some stats we keep.
  private long numAllowedRequests;
  private long numSoftDenies;
  private long numHardDenies;

  public Bucket(RateLimitConfig.Bucket config) {
    if (config.getSoftLimit().qps > 0) {
      this.soft = new Limit(config.getSoftLimit());
    } else {
      this.soft = null;
    }
    if (config.getHardLimit().qps > 0) {
      this.hard = new Limit(config.getHardLimit());
    } else {
      this.hard = null;
    }
    this.delayMs = config.getDelayMs();
    this.maxRequestsPerHour = config.getMaxRequestsPerHour();
    this.lastRequestTime = System.currentTimeMillis();
  }

  /**
   * Gets the number of milliseconds to delay the request by, or 0 if the request should be not
   * delayed.
   *
   * @return Number of milliseconds to delay the request by, not 0 if no delay is required.
   * @throws RequestException If the request should be rejected outright.
   */
  public long delayRequest(Session session, HttpServletRequest request) throws RequestException {
    synchronized (lock) {
      if (maxRequestsPerHour > 0) {
        DateTime hour = DateTime.now().withTime(DateTime.now().getHourOfDay(), 0, 0, 0);
        if (currentHour == null || !currentHour.equals(hour)) {
          currentHour = hour;
          numRequestsThisHour = 0;
        }
        numRequestsThisHour++;
        if (numRequestsThisHour > maxRequestsPerHour) {
          // If you hit the max limit in one hour, you'll be locked out for the whole hour, even
          // if you slow down.
          throw new RequestException(429, "Rate limit exceeded.");
        }
      }

      long msSinceLastRequest = System.currentTimeMillis() - lastRequestTime;
      lastRequestTime = System.currentTimeMillis();
      boolean softDeny = soft != null && !soft.allow(msSinceLastRequest);
      boolean hardDeny = hard != null && !hard.allow(msSinceLastRequest);

      if (hardDeny) {
        // If it's a hard deny, throw an exception with a status code indicating as such.
        numHardDenies ++;
        log.info("Hard deny %s %s for [%d]: %s",
            request.getMethod(), request.getPathInfo(), session.getEmpireID(), hard);
        throw new RequestException(429, "Rate limit exceeded.");
      }
      else if (softDeny) {
        numSoftDenies ++;
        log.info("Soft deny %s %s for [%d]: %s",
            request.getMethod(), request.getPathInfo(), session.getEmpireID(), soft);
        return delayMs;
      }

      numAllowedRequests ++;
      return 0;
    }
  }
}
