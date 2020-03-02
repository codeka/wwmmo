package au.com.codeka.warworlds.server.monitor.ratelimit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import au.com.codeka.warworlds.server.Configuration;

public class RateLimitConfig {
  @Expose List<Bucket> buckets = new ArrayList<>();
  @Expose Bucket defaultBucket;

  public static RateLimitConfig load() {
    File file = new File(Configuration.i.getConfigDirectory(), "rate-limit.json");
    if (!file.exists()) {
      return new RateLimitConfig();
    }

    Gson gson = new GsonBuilder().create();
    try {
      JsonReader jsonReader = new JsonReader(new FileReader(file));
      jsonReader.setLenient(true); // allow comments (and a few other things)
      return gson.fromJson(jsonReader, RateLimitConfig.class);
    } catch (IOException e) {
      return new RateLimitConfig();
    }
  }

  public List<Bucket> getBuckets() {
    return buckets;
  }

  /**
   * Get the default bucket that we apply to everybody. Or null if none is configured.
   */
  @Nullable
  public Bucket getDefaultBucket() {
    return defaultBucket;
  }

  static class BucketLimit {
    @Expose double qps = 1.0;
    @Expose int size = 10;

    public double getQps() {
      return qps;
    }

    public int getSize() {
      return size;
    }

  }

  static class Bucket {
    @Expose List<Integer> empireIds = new ArrayList<>();
    @Expose BucketLimit softLimit = new BucketLimit();
    @Expose BucketLimit hardLimit = new BucketLimit();
    @Expose long delayMs = 2000;
    @Expose int maxRequestsPerHour = 0;

    public List<Integer> getEmpireIds() {
      return empireIds;
    }

    public BucketLimit getSoftLimit() {
      return softLimit;
    }

    public BucketLimit getHardLimit() {
      return hardLimit;
    }

    public long getDelayMs() {
      return delayMs;
    }

    public int getMaxRequestsPerHour() {
      return maxRequestsPerHour;
    }
  }
}
