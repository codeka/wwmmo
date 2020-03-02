package au.com.codeka.warworlds.server.metrics;

import com.codahale.metrics.MetricRegistry;

public class MetricsManager {
  public static final MetricsManager i = new MetricsManager();

  private final MetricRegistry metrics = new MetricRegistry();

  private MetricsManager() {
  }

  public MetricRegistry getMetricsRegistry() {
    return metrics;
  }

  public void start() {
  }

  public void stop() {
  }
}
