package au.com.codeka.warworlds.server.monitor.ratelimit;

import com.google.api.client.util.Objects;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;

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
    long delayMs;

    Limit(RateLimitConfig.BucketLimit config) {
      qps = config.getQps();
      maxSize = config.getSize();
      delayMs = config.getDelayMs();
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
          .add("delayMs", delayMs)
          .toString();
    }
  }

  private long lastRequestTime;
  private final Limit hard;
  private final Limit soft;

  // Some stats we keep.
  long numAllowedRequests;
  long numSoftDenies;
  long numHardDenies;

  public Bucket(RateLimitConfig.Bucket config) {
    this.soft = new Limit(config.getSoftLimit());
    this.hard = new Limit(config.getHardLimit());
    this.lastRequestTime = System.currentTimeMillis();
  }

  /**
   * Gets the number of milliseconds to delay the request by, or 0 if the request should be not
   * delayed.
   *
   * @return Number of milliseconds to delay the request by, not 0 if no delay is required.
   * @throws RequestException If the request should be rejected outright.
   */
  public long delayRequest() throws RequestException {
    synchronized (lock) {
      long msSinceLastRequest = System.currentTimeMillis() - lastRequestTime;
      lastRequestTime = System.currentTimeMillis();
      boolean softDeny = !soft.allow(msSinceLastRequest);
      boolean hardDeny = !hard.allow(msSinceLastRequest);

      if (hardDeny) {
        // If it's a hard deny, throw an exception with a status code indicating as such.
        numHardDenies ++;
        throw new RequestException(429, "Rate limit exceeded.");
      }
      else if (softDeny) {
        numSoftDenies ++;
        log.info("soft deny for limit: %s", soft);
        return soft.delayMs;
      }

      numAllowedRequests ++;
      return 0;
    }
  }
}
